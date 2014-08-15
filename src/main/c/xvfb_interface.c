

#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <string.h>

#include <sys/mman.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>

#include <errno.h>

#include <X11/XWDFile.h>

// from http://cvsweb.xfree86.org/cvsweb/*checkout*/xc/programs/Xserver/hw/vfb/InitOutput.c?rev=HEAD&content-type=text/plain
// XXX: Going by the source, it looks correct, but it may contain bugs with long hostnames
#define XWD_WINDOW_NAME_LEN 60

typedef struct _xvfb_interface_context
{
  FILE* mapping;
  void* memory;
  size_t size;
  void* target_memory;
  
  int header_size;
  int image_width;
  int image_height;
  
} xvfb_interface;

CARD32 readValue(void* address) 
{
  char* data = (char*) address;
  CARD32 value = (data[3]<<0) | (data[2]<<8) | (data[1]<<16) | (data[0]<<24);
  return value;
  
}

int get_header_size(XWDFileHeader* header)
{
  // the graphics data should lie past this...
    int header_size = readValue(&header->header_size);
    int colourmap_size = readValue(&header->ncolors) * sizeof(XWDColor);
    
    // I didn't trust the header size data from the header
    header_size = sizeof(XWDFileHeader);
    return header_size + colourmap_size + XWD_WINDOW_NAME_LEN;
}

xvfb_interface* xvfb_interface_init(char* frameBufferPath)
{
  xvfb_interface* interface = malloc(sizeof(xvfb_interface));
  FILE* file = fopen(frameBufferPath, "r");
  if(file == NULL)
  {
    printf("Could not open file: %s: %s\n", frameBufferPath, strerror(errno));
    return NULL;
  }
  int fd = fileno(file);
  
  // get the size of the Xvfb output file
  struct stat stat_buf;
  fstat(fd, &stat_buf);
  size_t size = stat_buf.st_size;
  
  void* memory = mmap(NULL, size, PROT_READ, MAP_SHARED, fd, 0);
  
  if(memory == (void*)-1)
  {
     printf("Could not mmap memory: %s\n", strerror(errno));
     return NULL;
  }
  
  interface->memory = memory;
  interface->mapping = file;
  interface->size = size;
  interface->target_memory = malloc(size);
  
  // record some values from the header, to allow us to check for corrupt frames later
  XWDFileHeader* header = (XWDFileHeader*) interface->memory;
  
  interface->image_width = readValue(&header->pixmap_width);
  interface->image_height = readValue(&header->pixmap_height);
  interface->header_size = get_header_size(header);
  return interface;
}

char* xvfb_interface_sanityChecks(xvfb_interface* xvfbInterface)
{
  XWDFileHeader* header = (XWDFileHeader*) xvfbInterface->memory;
  
  struct stat stat_buf;
  fstat(fileno(xvfbInterface->mapping), &stat_buf);
  
  if(stat_buf.st_size != xvfbInterface->size)
  {
    printf("ERROR: XWD file has changed size: Was %d now %d\n", xvfbInterface->size, stat_buf.st_size);
    return "XWD file has changed size!";
  }
  
  if(xvfbInterface->size < sizeof(XWDFileHeader))
  {
    return "XWD file is too small to be the output of a Xvfb server";
  }
  
  int file_version = readValue(&header->file_version);
  if(file_version != XWD_FILE_VERSION)
  {
    printf("ERROR: File format hex was: %x\n", file_version);
    return "Not in the XWD file format!";
  }
  
  int bit_depth = readValue(&header->bits_per_pixel);
  
  // Check bit depth - passing "-screen 0 1280x1024x24" to Xvfb actually gives us 32 bits per pixel - we just don't use the Alpha
  if(bit_depth != 32)
  {
    return "Incorrect bit depth - you must set the Xvfb server to use 24 bits per pixel (8 bits per-colour, no alpha)";
  }
  
  int width = xvfb_interface_getWidth(xvfbInterface);
  int height = xvfb_interface_getHeight(xvfbInterface);
  
  int expected_file_size = width * height * 2 + get_header_size(header);
  
  if(xvfbInterface->size < expected_file_size)
  {
    return "The XWD file is unexpectedly small - this will probably crash the encoder (now aborting)";
  }
  
  return NULL;
}


int xvfb_interface_getWidth(xvfb_interface* interface)
{
  return interface->image_width;
}
int xvfb_interface_getHeight(xvfb_interface* interface)
{
  return interface->image_height;
}

void* xvfb_interface_getScreenshot(xvfb_interface* interface)
{    
    // First we take a snapshot of the Xvfb output
    memcpy(interface->target_memory, interface->memory, interface->size);
    
    // Then we start verifying the snapshot, to see if it is corrupted (happens in rare cases)
    XWDFileHeader* header = (XWDFileHeader*) interface->target_memory;

    int header_size = get_header_size(header);
    
    if(header_size != interface->header_size)
    {
      printf("ERROR: Header size incorrect! Is %d (corrupt input?)\n", header_size);
      fflush(NULL);
      return NULL;
    }
    
    int width = readValue(&header->pixmap_width);
    int height = readValue(&header->pixmap_height);
    
    if(width != interface->image_width)
    {
        printf("ERROR: Image width in header is incorrect (corrupt input?)\n");
        fflush(NULL);
        return NULL;
    }
    if(height != interface->image_height)
    {
        printf("ERROR: Image height in header is incorrect (corrupt input?)\n");
        fflush(NULL);
        return NULL;
    }
   
    return interface->target_memory + header_size;
}


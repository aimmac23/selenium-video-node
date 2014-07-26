

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

typedef struct _xvfb_interface_context
{
  FILE* mapping;
  void* memory;
  size_t size;
  
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
    
    return header_size + colourmap_size;
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
  return interface;
}

char* xvfb_interface_sanityChecks(xvfb_interface* xvfbInterface)
{
  XWDFileHeader* header = (XWDFileHeader*) xvfbInterface->memory;
  
  if(xvfbInterface->size < sizeof(XWDFileHeader))
  {
    return "XWD file is too small to be the output of a Xvfb server";
  }
  
  int file_version = readValue(&header->file_version);
  if(file_version != XWD_FILE_VERSION)
  {
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
  XWDFileHeader* header = (XWDFileHeader*) interface->memory;
  return readValue(&header->pixmap_width);
}
int xvfb_interface_getHeight(xvfb_interface* interface)
{
  XWDFileHeader* header = (XWDFileHeader*) interface->memory;
  return readValue(&header->pixmap_height);
}

void* xvfb_interface_getScreenshot(xvfb_interface* interface)
{
    XWDFileHeader* header = (XWDFileHeader*) interface->memory;
    
    return interface->memory + get_header_size(header);

}


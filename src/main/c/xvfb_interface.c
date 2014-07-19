

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
  
  
} xvfb_interface;

CARD32 readValue(void* address) 
{
  char* data = (char*) address;
  CARD32 value = (data[3]<<0) | (data[2]<<8) | (data[1]<<16) | (data[0]<<24);
  return value;
  
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
  return interface;
}

int xvfb_interface_getWidth(xvfb_interface* interface)
{
  XWDFileHeader* header = (XWDFileHeader*) interface->memory;
  // XXX: This bitshift is possibly incorrect
  return readValue(&header->pixmap_width);
}
int xvfb_interface_getHeight(xvfb_interface* interface)
{
  XWDFileHeader* header = (XWDFileHeader*) interface->memory;
  // XXX: This bitshift is possibly incorrect
  return readValue(&header->pixmap_height);
}

void* xvfb_interface_getScreenshot(xvfb_interface* interface)
{
    XWDFileHeader* header = (XWDFileHeader*) interface->memory;
    // the graphics data should lie past this...
    int header_size = readValue(&header->header_size);
    
    return interface->memory + header_size;

}


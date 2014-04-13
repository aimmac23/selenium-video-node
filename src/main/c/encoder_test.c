
#include "encoder_interface.c"

       #include <sys/types.h>
       #include <sys/stat.h>
       #include <unistd.h>

       
void fatal(const char* error, ...)
{
  printf("FATAL: %s\n", error);
  exit(1);
}

int main()
{
  printf("starting test\n");
  FILE* input = fopen("screenshot.bin", "rb");
  if(input == NULL)
  {
    printf("Couldn't find file\n");
    exit(1);
  }
  
  encoder_context* context = create_context();
  init_encoder(context, 1366, 768);
  init_codec(context);
  init_image(context);
  
  struct stat stat_info;
  int result = stat("screenshot.bin", &stat_info);
  if(result)
  {
    fatal("Could not fstat");
  }
  
  char* buffer = malloc(stat_info.st_size);
  fread(buffer, stat_info.st_size, 1, input);
  
  /*
  memset(buffer, 0xF, stat_info.st_size);
  
  int a;
  for(a = 0; a < stat_info.st_size; a++)
  {
    if(a % 4 == 0)
    {
      buffer[a] = 0x0;
    }
  }*/
  
  convert_frame(context, buffer);
  FILE* output = fopen("mem.bin", "wb");
  fwrite(context->raw->planes[2], 100000, 1, output);
  fflush(output);
  printf("Size is %d\n", context->raw->stride[3]);

  int i;
  for(i = 0; i < 100; i++)
  {
  convert_frame(context, buffer);
  
  encode_next_frame(context);
  }
  encode_finish(context);
  
  printf("Finished test\n");
}


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
   FILE*  xwd_file;

   void*  source_memmap;

   size_t input_size;

   void*  target_memory;

   unsigned header_size;

   unsigned xwdfilehdr_size;
   unsigned xwdfile_version;

   unsigned image_width;
   unsigned image_height;

   unsigned ncolors_hdr;

   unsigned colormap_size;

   unsigned bits_per_pixel;

} xvfb_interface;


unsigned readValue( void* address )
{
   unsigned char* data = ( unsigned char* )address;

   unsigned value = ( ( ( unsigned )data[ 3 ] ) | ( ( ( unsigned )data[ 2 ] ) << 8 ) | ( ( ( unsigned )data[ 1 ] ) << 16 ) | ( ( ( unsigned )data[ 0 ] ) << 24 ) );

   return value;
}


unsigned get_header_size( XWDFileHeader* header )
{
   // the graphics data should lie past this...

   unsigned header_size = readValue( &header->header_size );

   unsigned colourmap_size = readValue( &header->ncolors ) * sizeof( XWDColor );

   return header_size + colourmap_size;
}


xvfb_interface* xvfb_interface_init( char* pathToFrameBuffer )
{
   xvfb_interface* interface = ( xvfb_interface* )malloc( sizeof( xvfb_interface ) );

   memset( interface, 0, sizeof( xvfb_interface ) );  // ++paranoia

   FILE* input_file = fopen( pathToFrameBuffer, "r" );

   if ( NULL == input_file )
   {
      free( interface );

      printf( "Could not open file: %s: %s\n", pathToFrameBuffer, strerror( errno ) );

      return NULL;
   }

   int input_fd = fileno( input_file );

   struct stat file_info;

   fstat( input_fd, &file_info );  // get the input_size of the Xvfb output file

   size_t file_size = file_info.st_size;

   // MAP_PRIVATE since we don't want to commit the changes back to the file and source_mem_map is cheap ;^}

   void* memory = mmap( NULL, file_size, PROT_READ, MAP_PRIVATE, input_fd, 0 );

   if ( ( ( void* )-1 ) == memory )
   {
      printf( "Could not mmap source xwd input file: %s\n", strerror( errno ) );

      fclose( input_file );
      free( interface );

      return NULL;
   }

   interface->source_memmap = memory;
   interface->xwd_file = input_file;
   interface->input_size = file_size;

   interface->target_memory = malloc( file_size );

   if ( NULL == interface->target_memory )
   {
      printf( "Failed to allocate %u bytes to duplicate virtual XWindows frame buffer\n", file_size );

      munmap( memory, file_size );
      fclose( input_file );
      free( interface );

      return NULL;
   }

   // record some values from the header, to allow us to check for corrupt frames later

   XWDFileHeader* header = ( XWDFileHeader* )interface->source_memmap;

   interface->header_size = get_header_size( header );

   interface->xwdfilehdr_size = readValue( &header->header_size );

   interface->xwdfile_version = readValue( &header->file_version );

   interface->image_width = readValue( &header->pixmap_width );
   interface->image_height = readValue( &header->pixmap_height );

   interface->bits_per_pixel = readValue( &header->bits_per_pixel );

   interface->ncolors_hdr = readValue( &header->ncolors );

   interface->colormap_size = sizeof( XWDColor ) * interface->ncolors_hdr;

   return interface;
}


unsigned xvfb_interface_getWidth( xvfb_interface* xvfb )
{
   return xvfb->image_width;
}


unsigned xvfb_interface_getHeight( xvfb_interface* xvfb )
{
   return xvfb->image_height;
}


char* xvfb_interface_sanityChecks( xvfb_interface* xvfb )
{
   static char errorMSG[ 2048 ] = { 0 };

   XWDFileHeader* header = ( XWDFileHeader* )xvfb->source_memmap;

   struct stat file_info;

   fstat( fileno( xvfb->xwd_file ), &file_info );

   if ( file_info.st_size != xvfb->input_size )
   {
      sprintf( errorMSG, "ERROR: XWD file has changed input_size!: (expected: %u bytes, actual: %u bytes)\n", xvfb->input_size, file_info.st_size );

      return errorMSG;
   }

   if ( file_info.st_size < sizeof( XWDFileHeader ) )
   {
      return "ERROR: XWD file is too small to be the valid output of a Xvfb server!";
   }

   unsigned file_version = readValue( &header->file_version );

   if ( file_version != XWD_FILE_VERSION )
   {
      sprintf( errorMSG, "ERROR: Incorrect XWD file format version (expected: %x, actual: %x)!", XWD_FILE_VERSION, xvfb->xwdfile_version );

      return errorMSG;
   }

   unsigned bits_per_pixel = readValue( &header->bits_per_pixel );

   // Check pixel depth - passing "-screen 0 1280x1024x24" to Xvfb actually gives us 32 bits per pixel - we just don't use the Alpha
   // ED: XWDFileHeader->pixmap_depth contains the last value in the width x height x depth triplet

   if ( 32 != bits_per_pixel )
   {
      return "Incorrect pixel depth - you must set the Xvfb server to use a minimum of 24 bits per pixel (8 bits per-color, no alpha)";
   }

   unsigned expected_file_size = ( xvfb->bits_per_pixel / 8 ) * xvfb->image_width * xvfb->image_height + xvfb->xwdfilehdr_size + xvfb->colormap_size;

   if ( file_info.st_size < expected_file_size )
   {
      sprintf( errorMSG, "ERROR: The XWD file is unexpectedly small (expected: %u bytes, actual: %u bytes ) - this will probably crash the encoder, now aborting!", expected_file_size, file_info.st_size );

      return errorMSG;
   }

  return NULL;
}


void* xvfb_interface_getScreenshot( xvfb_interface* xvfb )
{
   // take a snapshot of the Xvfb output
   memcpy( xvfb->target_memory, xvfb->source_memmap, xvfb->input_size );

   // perform sanity checks on the snapshot to verify if it is corrupted (happens in rare cases)
   XWDFileHeader* header = ( XWDFileHeader* )xvfb->target_memory;

   unsigned header_size = get_header_size( header );

   if( header_size != xvfb->header_size )
   {
      printf( "ERROR: Header size incorrect: (expected: %u bytes, actual: %u bytes )!  Is the input file corrupt?\n", xvfb->header_size, header_size );

      fflush( NULL );

      return NULL;
   }

   unsigned width = readValue( &header->pixmap_width );
   unsigned height = readValue( &header->pixmap_height );

   if ( width != xvfb->image_width )
   {
      printf( "ERROR: Image width in header is incorrect (corrupt input?)\n" );

      fflush( NULL );

      return NULL;
   }

   if ( height != xvfb->image_height )
   {
      printf( "ERROR: Image height in header is incorrect (corrupt input?)\n" );

      fflush( NULL );

      return NULL;
   }

   return xvfb->target_memory + header_size;
}

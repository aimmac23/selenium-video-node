#define VPX_CODEC_DISABLE_COMPAT 1
#include "vpx/vpx_encoder.h"
#include "vpx/vp8cx.h"
#define interface (vpx_codec_vp8_cx())
//#define fourcc    0x30385056

#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <string.h>

#include "libyuv.h"

#include "webmenc.h"

typedef struct _encoder_context
{
      vpx_codec_enc_cfg_t  cfg;
      vpx_codec_ctx_t      codec;
      vpx_image_t*          raw;
      int width;
      int height;
      vpx_codec_pts_t frame_count;
      FILE* output;
      struct EbmlGlobal encoder_output;
  
} encoder_context;

       
void fatal(const char* error, ...)
{
  printf("FATAL: %s\n", error);
  exit(1);
}


encoder_context* create_context()
{
  encoder_context* context = malloc(sizeof(encoder_context));
  memset(context, 0, sizeof(encoder_context));
  
  context->output = fopen("output.mkv", "wb");
  
  if(!context->output)
  {
    printf("Couldn't open output file, but continuing anyway :/");
  }
  
  context->encoder_output.stream = context->output;
  context->encoder_output.debug = 1;
  context->encoder_output.cue_list = NULL;

  return context;
  
}

int init_encoder(encoder_context* context, int width, int height)
{
  
  int result = vpx_codec_enc_config_default(interface, &context->cfg, 0);
  
  if(result)
  {
    return result;
  }
  // XXX: The example does this before setting width/height - not sure why
  context->cfg.rc_target_bitrate = width * height * context->cfg.rc_target_bitrate / context->cfg.g_w / context->cfg.g_h;
  context->cfg.g_w = width;
  context->cfg.g_h = height;
  
  context->width = width;
  context->height = height;
  
  context->frame_count = 0;
  
  printf("Video stats: width: %d height: %d bitrate: %d\n", context->cfg.g_w, context->cfg.g_h, context->cfg.rc_target_bitrate);
  
  write_webm_file_header(&context->encoder_output, &context->cfg, &context->cfg.g_timebase, STEREO_FORMAT_MONO, VP8_FOURCC);
  
  return result;
}

int init_codec(encoder_context* context)
{
  return vpx_codec_enc_init(&context->codec, interface, &context->cfg, 0);
}
  
int init_image(encoder_context* context)
{
  context->raw = vpx_img_alloc(NULL, VPX_IMG_FMT_I420, context->width, context->height, 1);
  return context->raw == NULL;
}

int convert_frame(encoder_context* context, const uint8* data) 
{
  int inputSize = context->width * context->height * 4; // 4 bytes in an int
  
  vpx_image_t* image = context->raw;
  
  int result = ARGBToI420(data, context->width * 4, // appears to be a 4 byte format?
                  image->planes[0], image->stride[0], // Y Plane
                  image->planes[1], image->stride[1], // U plane
                  image->planes[2], image->stride[2], // V plane
                  context->width, context->height);
  
  return result;
}

int do_encode(encoder_context* context, vpx_image_t* image, unsigned long duration)
{
  int result = vpx_codec_encode(&context->codec, image, context->frame_count,
                                duration, 0, VPX_DL_REALTIME);
  if(result) 
  {
    return result;
  }
  vpx_codec_iter_t iterator = NULL;
  const vpx_codec_cx_pkt_t* packet = NULL;
  while((packet = vpx_codec_get_cx_data(&context->codec, &iterator)) != NULL)
  {
    switch(packet->kind) 
    {
      case VPX_CODEC_CX_FRAME_PKT:
//	fprintf(stderr, "Got frame packet\n");
	write_webm_block(&context->encoder_output, &context->cfg, packet);
	//fprintf(stderr, "After frame packet\n");
        break;
      default:
	// we can also receive statistics packets
        break;
    }
  }
  context->frame_count++;
  return 0;
}

int encode_next_frame(encoder_context* context, unsigned long duration)
{
  return do_encode(context, context->raw, duration);
}

int encode_finish(encoder_context* context)
{
  int result = 0;
  // pass NULL, to signal that we're done
  result = do_encode(context, NULL, 1);
  
  // XXX: 0 is an incorrect hash
  write_webm_file_footer(&context->encoder_output, 1);
  fflush(context->output);
  fclose(context->output);
  
  return result;
}

const char* codec_error_detail(encoder_context* context)
{
  return vpx_codec_error_detail(&context->codec);
}
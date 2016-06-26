#include "vpxenc.h"
#include "tools_common.h"
#include "video_writer.h"

#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <string.h>

// libyuv functions
#include "libyuv/convert.h"

#include "webmenc.h"

typedef struct _encoder_context
{
      const VpxInterface *encoder;
      VpxVideoInfo video_info;
      VpxVideoWriter* writer;
      // older
      vpx_codec_enc_cfg_t  cfg;
      vpx_codec_ctx_t      codec;
      vpx_image_t *         raw;
      int width;
      int height;
      char* filename;
      vpx_codec_pts_t pts_timestamp;
      FILE* output;
  
} encoder_context;

       
void fatal(const char* error, ...)
{
  printf("FATAL: %s\n", error);
  exit(1);
}


encoder_context* create_context(char* output_file)
{
  encoder_context* context = (encoder_context*) malloc(sizeof(encoder_context));
  memset(context, 0, sizeof(encoder_context));
  
  strcpy(context->filename, output_file);
  
  return context;
  
}

int init_encoder(encoder_context* context, int width, int height, int fps)
{
  int result = 0;
  context->encoder = get_vpx_encoder_by_name("vp8");
  
  if(!context->encoder) 
  {
    die("Couldn't get encoder context");
    return -1;
  }
  
  context->video_info.frame_width = width;
  context->video_info.frame_height = height;
  
  context->video_info.time_base.numerator=1;
  context->video_info.time_base.denominator=fps;
  
  
  context->width = width;
  context->height = height;
  
  result = vpx_codec_enc_config_default(context->encoder->codec_interface(), &context->cfg, 0);
  if (result)
  {
      
      die("Failed to get default codec config.");
  }
  
  
  if (vpx_codec_enc_init(&context->codec, context->encoder->codec_interface(), &context->cfg, 0))
  {
      die("Failed to initialize encoder");
  }
  
  context->cfg.g_w = context->video_info.frame_width;
  context->cfg.g_h = context->video_info.frame_height;
  context->cfg.g_timebase.num = context->video_info.time_base.numerator;
  context->cfg.g_timebase.den = context->video_info.time_base.denominator;
  // TODO: From an older version - check its good
  context->cfg.rc_target_bitrate = width * height * context->cfg.rc_target_bitrate / context->cfg.g_w / context->cfg.g_h;
  
  context->writer = vpx_video_writer_open(context->filename, kContainerIVF, &context->video_info);
  if (!context->writer)
  {
    die("Failed to open %s for writing.", context->filename);  
  }
  
  return result;
}

int init_codec(encoder_context* context)
{
    // TODO: Do something here, or delete this method
}
  
int init_image(encoder_context* context)
{
  context->raw = vpx_img_alloc(NULL, VPX_IMG_FMT_I420, context->video_info.frame_width, context->video_info.frame_height, 1);
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
  int result = vpx_codec_encode(&context->codec, image, context->pts_timestamp,
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
          vpx_video_writer_write_frame(context->writer,
                                           packet->data.frame.buf,
                                           packet->data.frame.sz,
                                           packet->data.frame.pts);
        break;
      default:
	// we can also receive statistics packets
        break;
    }
  }
  // if we're hitting our target framerate, should be 1 - if not, take into account how long we displayed this frame for
  context->pts_timestamp += duration;
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
  
  vpx_video_writer_close(context->writer);
  fflush(context->output);
  fclose(context->output);
  
  // we also need to free some memory
  vpx_img_free(context->raw);
  
  //XXX: We should check the return result
  vpx_codec_destroy(&context->codec);
  
  free(context);
  
  return result;
}

const char* codec_error_detail(encoder_context* context)
{
  return vpx_codec_error_detail(&context->codec);
}

#define VPX_CODEC_DISABLE_COMPAT 1
#include "vpx/vpx_encoder.h"
#include "vpx/vp8cx.h"
#define interface (vpx_codec_vp8_cx())
#define fourcc    0x30385056

#include <stdlib.h>
#include <string.h>

typedef struct _encoder_context
{
      vpx_codec_enc_cfg_t  cfg;
      vpx_codec_ctx_t      codec;
      vpx_image_t          raw;
      int width;
      int height;
      vpx_codec_pts_t frame_count;
  
} encoder_context;

encoder_context* create_context()
{
  encoder_context* context = malloc(sizeof(encoder_context));
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
  
  return result;
}

int init_codec(encoder_context* context)
{
  return vpx_codec_enc_init(&context->codec, interface, &context->cfg, 0);
}
  
int init_image(encoder_context* context)
{
  // WAS: VPX_IMG_FMT_I420, but that's not what Java is using (on Linux)
  return vpx_img_alloc(&context->raw, VPX_IMG_FMT_RGB32_LE, context->width, context->height, 1) == 0;
}

int encode_frame(encoder_context* context, long* data)
{
  int targetSize = context->width * context->height * 4; // 4 bytes in an int
  memcpy(context->raw.planes[0], data, targetSize);
  int result = vpx_codec_encode(&context->codec, &context->raw, context->frame_count,
                                1, 0, VPX_DL_REALTIME);
  
  return result;
  
}

const char* codec_error_detail(encoder_context* context)
{
  return vpx_codec_error_detail(&context->codec);
}
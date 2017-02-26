
// need libx11-dev (on DEBIAN) and libxext-dev
#include <X11/X.h>
#include <X11/Xlib.h>
#include <X11/Xlibint.h>
#include <X11/Xproto.h>
#include <X11/Xutil.h>

#include <X11/extensions/XShm.h>

#include <sys/ipc.h>
#include <sys/shm.h>


#include <stdio.h>

typedef struct X11ScreenshotContext 
{
    Display* display;
    Screen* screen;
    
    int screen_num;
    int screen_width;
    int screen_height;    
    
    XShmSegmentInfo* shminfo;
    XImage *image;
    
    
} X11ScreenshotContext;

X11ScreenshotContext* x11_screenshot_source_init()
{
    // open the default display specified by the DISPLAY environment variable
    Display* display = XOpenDisplay(NULL);
    
    if(!display)
    {
        perror("Couldn't open display");
        return NULL;
    }
    
    if(!XShmQueryExtension(display))
    {
        perror("Shared memory extensions unavailable!");
        return NULL;
    }
    
    Screen* screen = DefaultScreenOfDisplay(display);
    
    if(!screen)
    {
        perror("Couldn't get default screen");
        return NULL;
    }
    
    
    int screen_width = WidthOfScreen(screen);
    int screen_height = HeightOfScreen(screen);
    
    int screen_num = DefaultScreen(display);
    
    // setup a shared memory segment and image, so the X11 server can write screenshot data into our address space
    XShmSegmentInfo* shminfo = malloc(sizeof(XShmSegmentInfo));
    
    XImage *image;
    
    image=XShmCreateImage(display,DefaultVisual(display,0), DefaultDepth(display, 0), ZPixmap,NULL,shminfo,screen_width,screen_height);
    
    if(!image)
    {
        perror("Couldn't create shared image");
        return NULL;
    }
    
    shminfo->shmid=shmget (IPC_PRIVATE,image->bytes_per_line*image->height,IPC_CREAT|0777);
    shminfo->shmaddr= image->data = shmat (shminfo->shmid, NULL, 0);
    shminfo->readOnly = False;
    
    if(!XShmAttach(display, shminfo)) 
    {
        perror("X Server couldn't attach to shared memory!");
        return NULL;
    }

    X11ScreenshotContext* toReturn = malloc(sizeof(X11ScreenshotContext));
    toReturn->display = display;
    toReturn->screen = screen;
    toReturn->screen_num = screen_num;
    toReturn->screen_width = screen_width;
    toReturn->screen_height = screen_height;
    toReturn->shminfo = shminfo;
    toReturn->image = image;
    
    
    return toReturn;
}

char* x11_screenshot_source_sanityChecks(X11ScreenshotContext* context)
{
    // take a screenshot, just to check that the image data is in the correct format
    if(!XShmGetImage (context->display, DefaultRootWindow(context->display), context->image, 0, 0, AllPlanes))
    {
        // from testing, if this step doesn't work its going to print out an X11 error and kill the process, rather than returning
        return "Unable to take screenshot";
    }
    
    switch(context->image->bits_per_pixel)
    {
        case 24:
            // I'm fairly sure this can be made to work, but I'd rather see a modern X11 server use it first.
            return "X11 Image is a 24-bit image. Please file a bug against this project to get support added for it";           
            
        case 32:
            // if the Pixels are ARGB/RGB:
            if (context->image->red_mask   == 0xff0000 && context->image->green_mask == 0x00ff00 && context->image->blue_mask  == 0x0000ff ) {
                //success!
                return NULL;
            }
            else 
            {
                // This doesn't have to be a fatal error, but the results are hilarious
                perror("X11 Image is a 32 bit image, but the pixel colour format is unrecognised. We will attempt to continue anyway, but the video colours may be completely incorrect");
                return NULL;   
            }
        default:
            return "Unrecognised X11 display bit depth";
    }
}

void* x11_screenshot_source_getScreenshot(X11ScreenshotContext* context)
{
    
    if(!XShmGetImage (context->display, DefaultRootWindow(context->display), context->image, 0, 0, AllPlanes))
    {
        perror("Couldn't get image data");
        return NULL;
    }
    
    return context->image->data;

}

void x11_screenshot_source_destroy(X11ScreenshotContext* context)
{
    // detach X11 from the shared memory, and release objects
    XShmDetach (context->display, context->shminfo);
    XDestroyImage (context->image);
    // close shared memory resources
    shmdt (context->shminfo->shmaddr);
    shmctl (context->shminfo->shmid, IPC_RMID, 0);
    // close the X Display connection
    XCloseDisplay(context->display);
    
}

int x11_screenshot_source_getWidth(X11ScreenshotContext* context)
{
   return context->screen_width; 
}

int x11_screenshot_source_getHeight(X11ScreenshotContext* context)
{
    return context->screen_height;
}

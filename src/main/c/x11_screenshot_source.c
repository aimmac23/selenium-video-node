
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

void x11_screenshot_source_init()
{
}

char* x11_screenshot_source_sanityChecks(void* context)
{
    
}

void* x11_screenshot_source_getScreenshot(void* context)
{
    
}

int main()
{
    // open the default display specified by the DISPLAY environment variable
    Display* display = XOpenDisplay(NULL);
    
    if(!display)
    {
        perror("Couldn't open display");
        return -1;
    }
    
    if(!XShmQueryExtension(display))
    {
        perror("Shared memory extensions unavailable!");
        return -1;
    }
    
    Screen* screen = DefaultScreenOfDisplay(display);
    
    if(!screen)
    {
        perror("Couldn't get default screen");
        return -1;
    }
    
    int screen_width = WidthOfScreen(screen);
    int screen_height = HeightOfScreen(screen);
    
    int screen_num = DefaultScreen(display);
    
    XShmSegmentInfo shminfo;
    
    XImage *image;
    
    image=XShmCreateImage(display,DefaultVisual(display,0), DefaultDepth(display, 0), ZPixmap,NULL,&shminfo,screen_width,screen_height);
    
    if(!image)
    {
        perror("Couldn't create shared image");
        return -1;
    }
    
    shminfo.shmid=shmget (IPC_PRIVATE,image->bytes_per_line*image->height,IPC_CREAT|0777);
    shminfo.shmaddr= image->data = shmat (shminfo.shmid, NULL, 0);
    shminfo.readOnly = False;
    
    if(!XShmAttach(display, &shminfo)) 
    {
        perror("X Server couldn't attach to shared memory!");
        return -1;
    }
    
    printf("Getting image - width: %d, height: %d\n", screen_width, screen_height);
    
    if(!XShmGetImage (display, DefaultRootWindow(display), image, 0, 0, AllPlanes))
    {
        perror("Couldn't get image data");
        return -1;
    }
    
    switch(image->bits_per_pixel)
    {
        case 24:
            printf("24 bit image\n");
            break;
            
            
        case 32:
            printf("32 bit image\n");
            // if the Pixels are ARGB/RGB:
            if (image->red_mask   == 0xff0000 && image->green_mask == 0x00ff00 && image->blue_mask  == 0x0000ff ) {
                printf("Correct!\n");
            }
            break;
    }
    
    
    // detach X11 from the shared memory, and release objects
    XShmDetach (display, &shminfo);
    XDestroyImage (image);
    // close shared memory resources
    shmdt (shminfo.shmaddr);
    shmctl (shminfo.shmid, IPC_RMID, 0);
    // close the X Display connection
    XCloseDisplay(display);
    
    printf("Success!\n");
}    

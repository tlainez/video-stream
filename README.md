# video-stream
Stream sample video and video/pictures from a system camera (proof of concept)

## Usage:

1. Start the application by running the main classs *com.VideoStreamApplication*

1. Enter the endpoint URL:

**Stream specific video file**

http://localhost:8080/webcam/stream/{fileType}/{fileName}

  Parameters: {fileType} e.g. "mp4"
            {fileName}: video file under resources folder
            
 **Take a photo using the webcam**
 
http://localhost:8080/webcam/takePhoto

 **Stream video from the webcam**
 
http://localhost:8080/webcam/captureVideo

The video size is predefined to 1MB but can be changed by modifyng the constant *CHUNK_VIDEO_SIZE*
The default camera is the first configured on the system and can be changed by modifying the constant *DEFAULT_CAMERA*

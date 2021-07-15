package com.services;

import com.constants.ModulabGlobals;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import javax.imageio.ImageIO;

@Service
public class VideoStreamService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  /**
   * Prepare the content.
   *
   * @param fileName String.
   * @param fileType String.
   * @param range    String.
   * @return ResponseEntity.
   */
  public ResponseEntity<byte[]> prepareContentFromVideo(String fileName, String fileType, String range) {
    long rangeStart = 0;
    long rangeEnd;
    byte[] data;
    Long fileSize;
    String fullFileName = fileName + "." + fileType;
    try {
      fileSize = getFileSize(fullFileName);
      if (range == null) {
        return ResponseEntity.status(HttpStatus.OK)
            .header(ModulabGlobals.CONTENT_TYPE, ModulabGlobals.VIDEO_CONTENT + fileType)
            .header(ModulabGlobals.CONTENT_LENGTH, String.valueOf(fileSize))
            .body(readByteRange(fullFileName, rangeStart, fileSize - 1)); // Read the object and convert it as bytes
      }
      String[] ranges = range.split("-");
      rangeStart = Long.parseLong(ranges[0].substring(6));
      if (ranges.length > 1) {
        rangeEnd = Long.parseLong(ranges[1]);
      } else {
        rangeEnd = fileSize - 1;
      }
      if (fileSize < rangeEnd) {
        rangeEnd = fileSize - 1;
      }
      data = readByteRange(fullFileName, rangeStart, rangeEnd);
    } catch (IOException e) {
      logger.error("Exception while reading the file {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
    String contentLength = String.valueOf((rangeEnd - rangeStart) + 1);
    return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
        .header(ModulabGlobals.CONTENT_TYPE, ModulabGlobals.VIDEO_CONTENT + fileType)
        .header(ModulabGlobals.ACCEPT_RANGES, ModulabGlobals.BYTES)
        .header(ModulabGlobals.CONTENT_LENGTH, contentLength)
        .header(ModulabGlobals.CONTENT_RANGE, ModulabGlobals.BYTES + " " + rangeStart + "-" + rangeEnd + "/" + fileSize)
        .body(data);
  }

public ResponseEntity<byte[]> takePictureFromCamera()
{
  FrameGrabber grabber = new OpenCVFrameGrabber(ModulabGlobals.DEFAULT_CAMERA);
  Frame frame;
  ByteArrayOutputStream baos = new ByteArrayOutputStream();

  try
  {
    grabber.start();
    frame = grabber.grab();

    BufferedImage image = new Java2DFrameConverter().convert(frame);
    baos = new ByteArrayOutputStream();
    ImageIO.write(image, "jpg", baos);
  }
  catch (FrameGrabber.Exception | FileNotFoundException e)
  {
    logger.error("Exception while reading the camera", e.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
  }
  catch (IOException e)
  {
    e.printStackTrace();
  }

 return ResponseEntity.status(HttpStatus.OK)
      .header(ModulabGlobals.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE)
      .header(ModulabGlobals.CONTENT_LENGTH, String.valueOf(baos.toByteArray().length))
      .body(baos.toByteArray());
}

  public ResponseEntity<byte[]> caprureVideoFromCamera()
  {
    FrameGrabber grabber = new OpenCVFrameGrabber(ModulabGlobals.DEFAULT_CAMERA);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(baos,256,256);

    try
    {
      grabber.start();
      recorder.setFormat("matroska");
      recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
      recorder.start();

      while (baos.size() < ModulabGlobals.CHUNK_VIDEO_SIZE)
      {
        recorder.record(grabber.grab());
      }
      recorder.stop();
    }
    catch (FrameGrabber.Exception | FFmpegFrameRecorder.Exception e)
    {
      logger.error("Exception while reading the camera", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
    return ResponseEntity.status(HttpStatus.OK)
        .header(ModulabGlobals.CONTENT_TYPE, ModulabGlobals.VIDEO_CONTENT + "mp4")
        .header(ModulabGlobals.CONTENT_LENGTH, String.valueOf(baos.toByteArray().length))
        .body(baos.toByteArray());
  }

  /**
   * ready file byte by byte.
   *
   * @param filename String.
   * @param start    long.
   * @param end      long.
   * @return byte array.
   * @throws IOException exception.
   */
  public byte[] readByteRange(String filename, long start, long end) throws IOException {
    Path path = Paths.get(getFilePath(), filename);
    try (InputStream inputStream = (Files.newInputStream(path));
        ByteArrayOutputStream bufferedOutputStream = new ByteArrayOutputStream()) {
      byte[] data = new byte[ModulabGlobals.BYTE_RANGE];
      int nRead;
      while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
        bufferedOutputStream.write(data, 0, nRead);
      }
      bufferedOutputStream.flush();
      byte[] result = new byte[(int) (end - start) + 1];
      System.arraycopy(bufferedOutputStream.toByteArray(), (int) start, result, 0, result.length);
      return result;
    }
  }

  /**
   * Get the filePath.
   *
   * @return String.
   */
  private String getFilePath() {
    URL url = this.getClass().getResource(ModulabGlobals.VIDEO);
    return new File(url.getFile()).getAbsolutePath();
  }

  /**
   * Content length.
   *
   * @param fileName String.
   * @return Long.
   */
  public Long getFileSize(String fileName) {
    return Optional.ofNullable(fileName)
        .map(file -> Paths.get(getFilePath(), file))
        .map(this::sizeFromFile)
        .orElse(0L);
  }

  /**
   * Getting the size from the path.
   *
   * @param path Path.
   * @return Long.
   */
  private Long sizeFromFile(Path path) {
    try {
      return Files.size(path);
    } catch (IOException ioException) {
      logger.error("Error while getting the file size", ioException);
    }
    return 0L;
  }
}

package com.controllers;

import com.services.VideoStreamService;

import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value =  "/video-stream")
public class VideoStreamController {

  private final VideoStreamService videoStreamService;

  public VideoStreamController(VideoStreamService videoStreamService) {
    this.videoStreamService = videoStreamService;
  }

  @GetMapping("/file/{fileType}/{fileName}")
  public Mono<ResponseEntity<byte[]>> streamVideo(ServerHttpResponse serverHttpResponse, @RequestHeader(value = "Range", required = false) String httpRangeList,
      @PathVariable("fileType") String fileType,
      @PathVariable("fileName") String fileName) {
    return Mono.just(videoStreamService.prepareContentFromVideo(fileName, fileType, httpRangeList));
  }

  @GetMapping("/camPhoto")
  public Mono<ResponseEntity<byte[]>> streamWebcamPicture() {
    return Mono.just(videoStreamService.takePictureFromCamera());
  }

  @GetMapping("/camVideo")
  public Mono<ResponseEntity<byte[]>> streamWebcamVideo() {
    return Mono.just(videoStreamService.caprureVideoFromCamera());
  }
}

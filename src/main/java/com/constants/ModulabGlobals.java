package com.constants;

public class ModulabGlobals
{
  public static final String BASLER_CAMERA_BALANCEWHITEAUTO_OFF = "BalanceWhiteAuto_Off";
  public static final String BASLER_CAMERA_BALANCEWHITEAUTO_ONCE = "BalanceWhiteAuto_Once";
  public static final String BASLER_CAMERA_BALANCEWHITEAUTO_CONTINUOUS = "BalanceWhiteAuto_Continuous";

  public static final String BASLER_CAMERA_EXPOSURE_OFF = "ExposureAuto_Off";
  public static final String BASLER_CAMERA_EXPOSURE_ONCE = "ExposureAuto_Once";
  public static final String BASLER_CAMERA_EXPOSURE_CONTINUOUS = "ExposureAuto_Continuous";

  public static final String BASLER_CAMERA_GAINAUTO_OFF = "GainAuto_Off";
  public static final String BASLER_CAMERA_GAINAUTO_ONCE = "GainAuto_Once";
  public static final String BASLER_CAMERA_GAINAUTO_CONTINUOUS = "GainAuto_Continuous";

  public static final String BASLER_CAMERA_SOURCEPRESETOFF = "LightSourcePreset_Off";
  public static final String BASLER_CAMERA_SOURCEPRESETDAYLIGHT5000K = "LightSourcePreset_Daylight5000K";
  public static final String BASLER_CAMERA_SOURCEPRESETDAYLIGHT6500K = "LightSourcePreset_Daylight6500K";
  public static final String BASLER_CAMERA_SOURCEPRESETTUNGSTEN2800K = "LightSourcePreset_Tungsten2800K";

  // 0: default camera, 1: next...so on
  public static final int DEFAULT_CAMERA = 1;
  public static final String VIDEO = "/video";
  public static final String CONTENT_TYPE = "Content-Type";
  public static final String CONTENT_LENGTH = "Content-Length";
  public static final String VIDEO_CONTENT = "video/";
  public static final String CONTENT_RANGE = "Content-Range";
  public static final String ACCEPT_RANGES = "Accept-Ranges";
  public static final String BYTES = "bytes";
  public static final int BYTE_RANGE = 1024;
  public static final int CHUNK_VIDEO_SIZE = 1024 * 10;
}

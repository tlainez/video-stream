package com.cameras.basler;

import com.constants.ModulabGlobals;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serializable;
import java.util.Observable;

public class BaslerModel extends Observable implements Serializable
{

  public static final String CHANGE_REASON_CONNECTION = "CHANGE_REASON_CONNECTION";
  public static final String CHANGE_REASON_ACQUISITION = "CHANGE_REASON_ACQUISITION";
  public static final String CHANGE_REASON_NEW_FRAME = "CHANGE_REASON_NEW_FRAME";
  public static final String CHANGE_REASON_PROPERTIES = "CHANGE_REASON_PROPERTIES";

  private static final String PREFS_PREFIX = "Basler";

  /**
   * Camera model connected. Set from native code
   */
  private String cameraModel;

  /**
   * <code>true</code> iff the connection to the camera has been established successfully
   */
  private boolean connected = false;

  /**
   * <code>true</code> iff the camera is acquiring frames
   */
  private boolean acquiring = false;

  /**
   * Last frame acquired by the camera. Access must be synchronized
   */
  private BufferedImage lastFrame = null;

  private int exposureTime;
  private String autoExposureMode;
  private String automaticGainControlMode;
  private double gain;
  private double sharpness;
  private double gamma;
  private boolean negative;
  private String lightSource = "";

  private int redLevel;
  private int greenLevel;
  private int blueLevel;
  private boolean continuousAWS;
  private int colorSaturation;
  private String whiteBalanceMode;

  /**
   * Image acquisition frame
   */
  private BaslerAcquisitionThread aBaslerAcquisitionThread = null;

  /**
   * Properties observer frame
   */
  private BaslerPropertiesObserverThread aBaslerPropertiesObserverThread = null;
  private int width;
  private int height;
  private Object testShortDescription;

  public BaslerModel()
  {
    super();
  }

  static
  {
    loadNativeLibrary();
  }

  private static void loadNativeLibrary()
  {
    String libraryPath = System.getProperty("user.dir") + File.separator + "Basler64.dll";
    try
    {
      System.load(libraryPath);
      // System.loadLibrary("Basler");
    }
    catch (Exception e)
    {
      System.out.println("Cannot find Basler.dll in path " + libraryPath);
    }
  }

  public double getModelGain()
  {
    return this.gain;
  }

  public int getModelExposureTime()
  {
    return this.exposureTime;
  }

  public String connect()
  {
    try
    {
      BaslerModel bm = new BaslerModel();

      // bm.startConnection();
      bm.openDevice();
      String test = bm.getCamModel();
      System.out.println(bm.getGamma());
      bm.setGamma(1D);
      System.out.println(bm.getGamma());
      bm.closeDevice();
      System.out.println(test);

      return "Basler camera connected successfully";
    }
    catch (Exception e)
    {
      System.out.println(e);
      return "ERROR connecting to a Basler camera";
    }
  }

  public void openDeviceNativeCaller()
  {
    this.openDevice();
  }

  public void startConnection()
  {

    this.setConnected(false);

    try
    {
      this.openDevice();
      if (this.getCamModel().length() > 0)
      {
        boolean temp = acquiring;
        getMomentaryCameraControl();
        width = getFrameWidthCaller();
        height = getFrameHeightCaller();
        this.acquiring = temp;
        this.setConnected(true);
      }
      else
      {
        throw new Exception();
      }
    }
    catch (Exception e)
    {
      System.out.println("Cannot connect to the Basler camera");
      System.out.println(e);
      try
      {
        this.closeDevice();
      }
      catch (Exception nothing)
      {
        // nothing
      }
    }

  }

  public String getCameraInfo()
  {
    if (this.isConnected())
    {
      boolean temp = acquiring;
      getMomentaryCameraControl();
      String camModel = this.getCamModel();
      acquiring = temp;
      return camModel;
    }
    else
    {
      return "BASLER_CONNECTION_STILL_NOT_ESTABLISHED"; // = Setting connection...
    }
  }

  public void endConnection()
  {
    System.out.println("Running BaslerAcquisitionThread");
    // Stop acquisition first
    this.stopAcquisition();
    this.closeDevice();
    setConnected(false);
  }

  public boolean isConnected()
  {
    return connected;
  }

  public void setConnected(boolean connected)
  {
    this.connected = connected;
    setChanged();
    notifyObservers(CHANGE_REASON_CONNECTION);
  }

  public boolean isAcquiring()
  {
    return acquiring;
  }

  public void setAcquiring(boolean acquiring)
  {
    this.acquiring = acquiring;
    setChanged();
    notifyObservers(CHANGE_REASON_ACQUISITION);
  }

  public void startAcquisition()
  {
    if (this.isConnected() && !this.isAcquiring())
    {
      this.setAcquiring(true);
      // this.restorePreferences();
      if (aBaslerAcquisitionThread == null || aBaslerAcquisitionThread.isFinished())
      {
        aBaslerAcquisitionThread = new BaslerAcquisitionThread(this);
        aBaslerAcquisitionThread.start();
      }
      // As the basler camera seems that can't inform about properties changes, this code is commented
      // if (aBaslerPropertiesObserverThread == null || aBaslerPropertiesObserverThread.isFinished())
      // {
      // aBaslerPropertiesObserverThread = new BaslerPropertiesObserverThread(this);
      // aBaslerPropertiesObserverThread.start();
      // }
    }
  }

  public void stopAcquisition()
  {
    if (this.isConnected() && this.isAcquiring())
    {
      if (aBaslerAcquisitionThread != null)
      {
        aBaslerAcquisitionThread.setFinished(true);
      }

      if (aBaslerPropertiesObserverThread != null)
      {
        aBaslerPropertiesObserverThread.setFinished(true);
      }

      this.setAcquiring(false);

    }
  }

  public synchronized Image getLastFrame()
  {
    return lastFrame;
  }

  public void captureFrame()
  {

    try
    {
      getMomentaryCameraControl();

      openDevice();
      byte[] frame = this.grabFrame();

      final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

      int n = 0;
      for (int i = 0; i < height; i++)
      {
        for (int j = 0; j < width; j++)
        {
          // Convert from byte to unsigned int
          int r = frame[n + 0] & 0xFF;
          int g = frame[n + 1] & 0xFF;
          int b = frame[n + 2] & 0xFF;

          // apply customer RGB levels
          double redFactor = r * (redLevel / 255D);
          double greenFactor = g * (greenLevel / 255D);
          double blueFactor = b * (blueLevel / 255D);
          r = (int) redFactor;
          g = (int) greenFactor;
          b = (int) blueFactor;

          Color rgb = new Color(r, g, b);

          image.setRGB(j, i, rgb.getRGB());

          n = n + 3;
        }
      }

      lastFrame = image;
      acquiring = true;
      setChanged();
      this.notifyObservers(CHANGE_REASON_NEW_FRAME);
    }
    catch (Exception e)
    {
      acquiring = true;
      System.out.println(e);
    }

  }

  public void acquireNewFrame()
  {

    if (this.isConnected() && this.isAcquiring())
    {
      // setExposureTimeCaller(500000);
      // System.out.println("Exposure time = " + getExposureTimeNativeCaller() + " us");
      // long start = System.currentTimeMillis();
      this.captureFrame();
      // long end = System.currentTimeMillis();
      // System.out.println("Frame acquired in " + (end - start) + " ms");
    }
  }

  // Getter for exposure time
  public int getExposureTimeNativeCaller()
  {
    boolean temp = acquiring;
    getMomentaryCameraControl();
    int exposureTimeTemp = this.getExposureTime() / 1000;
    acquiring = temp;
    return exposureTimeTemp;
  }

  // Setter for exposure time
  public void setExposureModeCaller(String exposureMode)
  {
    // Set the operation mode of the exposure auto function.
    // Possible values: "ExposureAuto_Off", "ExposureAuto_Once" and "ExposureAuto_Continuous"
    boolean temp = acquiring;
    getMomentaryCameraControl();
    autoExposureMode = exposureMode;

    switch (exposureMode)
    {
      case ModulabGlobals.BASLER_CAMERA_EXPOSURE_ONCE:
        this.setExposureAutoOnce();
        break;
      case ModulabGlobals.BASLER_CAMERA_EXPOSURE_CONTINUOUS:
        this.setExposureAutoContinuous();
        break;
      default:
        this.setExposureAutoOff();
    }
    acquiring = temp;
  }

  public String getModelAutoExposureMode()
  {
    return autoExposureMode;
  }

  public String getExposureAutoModeNativeCaller()
  {
    boolean temp = acquiring;
    getMomentaryCameraControl();
    String autoExposureModeTemp = this.getExposureAutoMode();
    acquiring = temp;
    return autoExposureModeTemp;
  }

  public void setExposureTimeCaller(Integer exposure)
  {
    boolean temp = acquiring;
    getMomentaryCameraControl();
    exposureTime = exposure;
    this.setExposureTime(exposure * 1000);
    acquiring = temp;
  }

  public void setContinuousAET()
  {
    boolean temp = acquiring;
    getMomentaryCameraControl();
    this.setExposureAutoContinuous();
    acquiring = temp;
  }

  public String getGainAutoModeNativeCaller()
  {
    // The operation mode of the gain auto function. Possible values: "GainAuto_Off", "GainAuto_Once" and "GainAuto_Continuous"
    boolean temp = acquiring;
    getMomentaryCameraControl();
    String automaticGainControlModeTemp = this.getGainAutoMode();
    acquiring = temp;
    return automaticGainControlModeTemp;
  }

  public String getModelAutomaticGainControl()
  {
    // The operation mode of the gain auto function. Possible values: "GainAuto_Off", "GainAuto_Once" and "GainAuto_Continuous"
    return automaticGainControlMode;
  }

  public void setAutomaticGainControl(String agcMode)
  {
    // The operation mode of the gain auto function. Possible values: "GainAuto_Off", "GainAuto_Once" and "GainAuto_Continuous"
    boolean temp = acquiring;
    getMomentaryCameraControl();
    this.automaticGainControlMode = agcMode;

    switch (agcMode)
    {
      case ModulabGlobals.BASLER_CAMERA_GAINAUTO_CONTINUOUS:
        this.setGainAutoContinuous();
        break;
      case ModulabGlobals.BASLER_CAMERA_GAINAUTO_ONCE:
        this.setGainAutoOnce();
        break;
      default:
        this.setGainAutoOff();
        this.setGainNativeCaller(gain);
    }
    acquiring = temp;
  }

  public double getGainNativeCaller()
  {
    boolean temp = acquiring;
    getMomentaryCameraControl();
    double gainTemp = this.getGain();
    acquiring = temp;
    return gainTemp;
  }

  public void setGainNativeCaller(double gain)
  {
    boolean temp = acquiring;
    getMomentaryCameraControl();
    this.gain = gain;
    this.setGain(gain);
    acquiring = temp;
  }

  public double getSharpnessEnhancementNativeCaller()
  {
    boolean temp = acquiring;
    getMomentaryCameraControl();
    double sharpnessEnhancementLevelTemp = this.getSharpnessEnhancementLevel();
    acquiring = temp;
    return sharpnessEnhancementLevelTemp;
  }

  public void setSharpnessEnhacementCaller(Double sharpness)
  {
    boolean temp = acquiring;
    this.sharpness = sharpness;
    getMomentaryCameraControl();
    this.setSharpnessEnhancementLevel(sharpness);
    acquiring = temp;
  }

  public double getModelSharpness()
  {
    return sharpness;
  }

  public double getGammaNativeCaller()
  {
    boolean temp = acquiring;
    getMomentaryCameraControl();
    double tempGamma = this.getGamma();
    acquiring = temp;
    return tempGamma;
  }

  public double getModelGamma()
  {
    return gamma;
  }

  public void setGammaNativeCaller(double gamma)
  {
    boolean temp = acquiring;
    getMomentaryCameraControl();
    this.gamma = gamma;
    setGamma(gamma);
    acquiring = temp;

  }

  public boolean isNegative()
  {
    return negative;
  }

  public void setNegative(boolean negative)
  {
    // this.nativeSetNegative(negative);
  }

  public String getModelLightSource()
  {
    return this.lightSource;
  }

  public String getLightSourceNativeCaller()
  {
    // Possible values: "LightSourcePreset_Off", "LightSourcePreset_Daylight5000K", "LightSourcePreset_Daylight6500K" and "LightSourcePreset_Tungsten2800K
    boolean temp = acquiring;
    getMomentaryCameraControl();
    String lightSourceTemp = getLightSourcePreset();
    acquiring = temp;
    return lightSourceTemp;
  }

  public void setLightSourceNativeCaller(String lightSource)
  {

    // Set the operation mode of the exposure auto function.
    // Possible values: "ExposureAuto_Off", "ExposureAuto_Once" and "ExposureAuto_Continuous"
    boolean temp = acquiring;
    getMomentaryCameraControl();
    this.lightSource = lightSource;
    switch (lightSource)
    {
      case ModulabGlobals.BASLER_CAMERA_SOURCEPRESETDAYLIGHT5000K:
        this.setLightSourcePresetDaylight5000K();
        break;
      case ModulabGlobals.BASLER_CAMERA_SOURCEPRESETDAYLIGHT6500K:
        this.setLightSourcePresetDaylight6500K();
        break;
      case ModulabGlobals.BASLER_CAMERA_SOURCEPRESETTUNGSTEN2800K:
        this.setLightSourcePresetTungsten2800K();
        break;
      default:
        this.setLightSourcePresetOff();

    }
    acquiring = temp;

  }

  public int getRedLevel()
  {
    return redLevel;
  }

  public void setRedLevel(int redLevel)
  {
    this.redLevel = redLevel;

  }

  public int getGreenLevel()
  {
    return greenLevel;
  }

  public void setGreenLevel(int greenLevel)
  {
    this.greenLevel = greenLevel;

  }

  public int getBlueLevel()
  {
    return blueLevel;
  }

  public void setBlueLevel(int blueLevel)
  {
    this.blueLevel = blueLevel;

  }

  public void oneTimeAWS()
  {
    // this.nativeOneTimeAWS();

  }

  public boolean getContinuousAWS()
  {
    // boolean temp = acquiring;
    // getMomentaryCameraControl();
    // acquiring = temp;
    return continuousAWS;
  }

  public void setContinuousAWS(boolean continuousAWS)
  {
    this.continuousAWS = continuousAWS;
  }

  public int getColorSaturation()
  {
    boolean temp = acquiring;
    getMomentaryCameraControl();
    acquiring = temp;
    return colorSaturation;
  }

  public void setColorSaturation(int colorSaturation)
  {
    this.colorSaturation = colorSaturation;
    // this.nativeSetColorSaturation(colorSaturation);
  }

  public int getFrameWidthCaller()
  {
    boolean temp = acquiring;
    getMomentaryCameraControl();
    int frameWidthTemp = this.getFrameWidth();
    acquiring = temp;
    return frameWidthTemp;
  }

  public int getFrameHeightCaller()
  {
    boolean temp = acquiring;
    getMomentaryCameraControl();
    int frameHeightTemp = this.getFrameHeight();
    acquiring = temp;
    return frameHeightTemp;
  }

  public void resetFactoryDefaultsCaller()
  {
    boolean temp = acquiring;
    getMomentaryCameraControl();

    resetFactoryDefaults();

    this.redLevel = 255;
    this.greenLevel = 255;
    this.blueLevel = 255;
    this.exposureTime = this.getExposureTimeNativeCaller();
    this.autoExposureMode = this.getExposureAutoModeNativeCaller();
    this.automaticGainControlMode = this.getGainAutoModeNativeCaller();
    this.gain = this.getGainNativeCaller();
    this.sharpness = this.getSharpnessEnhancementNativeCaller();
    this.gamma = this.getGammaNativeCaller();
    this.lightSource = this.getLightSourceNativeCaller();

    refreshProperties();
    acquiring = temp;
  }

  public String getModelWhiteBalanceMode()
  {
    return whiteBalanceMode;
  }

  public String getWhiteBalanceModeNativeCaller()
  {
    boolean temp = acquiring;
    getMomentaryCameraControl();
    String tempWhiteBalanceMode = this.getWhiteBalanceMode();
    // System.out.println("Native :" + tempWhiteBalanceMode);
    // System.out.println("Config :" + whiteBalanceMode);
    acquiring = temp;
    return tempWhiteBalanceMode;
  }

  public void setWhiteBalanceMode(String mode)
  {
    // Set the operation mode of the Whote mode function.
    // Possible values: "BalanceWhiteAuto_Off", "BalanceWhiteAuto_Once" and "BalanceWhiteAuto_Continuous"
    boolean temp = acquiring;
    getMomentaryCameraControl();
    whiteBalanceMode = mode;
    // System.out.println("Change white Mode: " + mode);
    switch (mode)
    {
      case "BalanceWhiteAuto_Once":
        this.setWhiteBalanceOnce();
        break;
      case "BalanceWhiteAuto_Continuous":
        this.setWhiteBalanceContinuous();
        break;
      default:
        this.setWhiteBalanceOff();
    }
    // System.out.println("Native after setMode: " + this.getWhiteBalanceMode());
    acquiring = temp;

  }

  void refreshProperties()
  {
    // if (this.nativeRefreshProperties())
    if (true)
    {
      this.setChanged();
      this.notifyObservers(CHANGE_REASON_PROPERTIES);
    }
  }

  private void getMomentaryCameraControl()
  {
    acquiring = false;
    openDevice();
    try
    {
      // Thread.sleep(100);
      while (this.isGrabbing())
      {
        Thread.sleep(50);
      }
    }
    catch (InterruptedException e)
    {
      acquiring = true;
      openDevice();
      // System.out.println("Error in procedure getMomentaryCameraControl");
      System.out.println(e);
    }

  }

  private void writeValuesToConsole(String testShortDescription, boolean activated)
  {

    if (!activated)
      return;

    boolean temp = acquiring;

    getMomentaryCameraControl();
    System.out.println("testShortDescription: " + testShortDescription);
    getMomentaryCameraControl();
    System.out.println("Exposure time: " + (this.getExposureTime() / 1000));
    getMomentaryCameraControl();
    System.out.println("Autoexposure: " + this.getExposureAutoMode());
    getMomentaryCameraControl();
    System.out.println("Gain: " + this.getGain());
    getMomentaryCameraControl();
    System.out.println("AutoGain: " + this.getGainAutoMode());
    getMomentaryCameraControl();
    System.out.println("Sharpness: " + this.getSharpnessEnhancementLevel());
    getMomentaryCameraControl();
    System.out.println("Gamma: " + this.getGamma());
    getMomentaryCameraControl();
    System.out.println("AutoWhite: " + this.getWhiteBalanceMode());
    getMomentaryCameraControl();
    System.out.println("Lightsource: " + this.getLightSourcePreset());
    getMomentaryCameraControl();
    System.out.println("************************");

    acquiring = temp;

  }

  // *************************************************************************************************************************************************
  // *************************************************************************************************************************************************
  // *************************************************************************************************************************************************

  // NATIVE METHODS

  // DEVICE MANAGEMENT

  /**
   * Initalizes the connection with the camera device
   */
  private native void openDevice();

  /**
   * Closes the connection with the camera device
   */
  private native void closeDevice();

  // INFO

  /**
   * @return Camera model
   */
  private native String getCamModel();

  // EXPOSURE TIME

  /**
   * @return The operation mode of the exposure auto function. Possible values: "ExposureAuto_Off", "ExposureAuto_Once" and "ExposureAuto_Continuous"
   */
  private native String getExposureAutoMode();

  /**
   * The exposure time auto function is disabled.
   */
  private native void setExposureAutoOff();

  /**
   * Exposure time is adjusted automatically until it reaches a specific target value.
   */
  private native void setExposureAutoOnce();

  /**
   * Exposure time is adjusted repeatedly while images are acquired.
   */
  private native void setExposureAutoContinuous();

  /**
   * @return The minimum allowed exposure time in microseconds.
   */
  private native int getMinExposureTime();

  /**
   * @return The maximum allowed exposure time in microseconds.
   */
  private native int getMaxExposureTime();

  /**
   * @return Exposure time of the camera in microseconds.
   */
  private native int getExposureTime();

  /**
   * @param exposureTime
   *          Exposure time of the camera in microseconds.
   */
  private native void setExposureTime(int exposureTime);

  // GRAB

  /**
   * @return RGB components of the image. The first two elements of the array represnets the image width and height.
   */
  private native int[] grabSizedFrame();

  /**
   * @return RGB components of the image.
   */
  private native byte[] grabFrame();

  /**
   * @return The minimum allowed frame width.
   */
  private native int getMinFrameWidth();

  /**
   * @return The maximum allowed frame width.
   */
  private native int getMaxFrameWidth();

  /**
   * @return Current frame width.
   */
  private native int getFrameWidth();

  /**
   * @param width
   *          frame width.
   */
  private native void setFrameWidth(int width);

  /**
   * @return The minimum allowed frame height.
   */
  private native int getMinFrameHeight();

  /**
   * @return The maximum allowed frame height.
   */
  private native int getMaxFrameHeight();

  /**
   * @return frame height.
   */
  private native int getFrameHeight();

  /**
   * @param height
   *          frame height.
   */
  private native void setFrameHeight(int height);

  // GAIN

  /**
   * @return The operation mode of the gain auto function. Possible values: "GainAuto_Off", "GainAuto_Once" and "GainAuto_Continuous"
   */
  private native String getGainAutoMode();

  /**
   * The gain auto function is disabled.
   */
  private native void setGainAutoOff();

  /**
   * Gain is adjusted automatically until it reaches a specific target value.
   */
  private native void setGainAutoOnce();

  /**
   * Gain is adjusted repeatedly while images are acquired.
   */
  private native void setGainAutoContinuous();

  /**
   * @return The minimum allowed gain control in dB.
   */
  private native double getMinGain();

  /**
   * @return The maximum allowed gain control in dB.
   */
  private native double getMaxGain();

  /**
   * @return Value of the currently selected gain control in dB.
   */
  private native double getGain();

  /**
   * @param gain
   *          Value of the currently selected gain control in dB.
   */
  private native void setGain(double gain);

  // SHARPNESS

  /**
   * @return Amount of sharpening to apply.
   */
  private native double getSharpnessEnhancementLevel();

  /**
   * @return The minimum allowed amount of sharpening to apply.
   */
  private native double getSharpnessEnhancementMinValue();

  /**
   * @return The maximum allowed amount of sharpening to apply.
   */
  private native double getSharpnessEnhancementMaxValue();

  /**
   * @param sharpnessLevel
   *          Amount of sharpening to apply. The higher the sharpness, the more distinct the image subject's contours will be. However, too high values may result in image information loss.
   */
  private native void setSharpnessEnhancementLevel(double sharpnessLevel);

  // NOISE REDUCTION

  /**
   * @return Amount of noise reduction to apply.
   */
  private native double getNoiseReductionLevel();

  /**
   * @return The minimum allowed amount of noise reduction to apply.
   */
  private native double getNoiseReductionMinValue();

  /**
   * @return The maximum allowed amount of noise reduction to apply.
   */
  private native double getNoiseReductionMaxValue();

  /**
   * @param noiseReductionLevel
   *          Amount of noise reduction to apply. The higher the value, the less chroma noise will be visible in your images. However, too high values may result in image information loss
   */
  private native void setNoiseReductionLevel(double noiseReductionLevel);

  // GAMMA

  /**
   * @return The minimum allowed amount of gamma correction value to apply.
   */
  private native float getGammaMinValue();

  /**
   * @return The maximum allowed amount of gamma correction value to apply.
   */
  private native float getGammaMaxValue();

  /**
   * @return Gamma correction value to apply.
   */
  private native double getGamma();

  /**
   * @param gamma
   *          Gamma correction value to apply. Gamma correction lets you modify the brightness of the pixel values to account for a non-linearity in the human perception of brightness.
   */
  private native void setGamma(double gamma);

  // WHITE BALANCE

  /**
   * @return The operation mode of the balance white auto function. Possible values: "BalanceWhiteAuto_Off", "BalanceWhiteAuto_Once" and "BalanceWhiteAuto_Continuous"
   */
  private native String getWhiteBalanceMode();

  /**
   * The balance white auto function is disabled.
   */
  private native void setWhiteBalanceOff();

  /**
   * White balance is adjusted automatically until it reaches a specific target value.
   */
  private native void setWhiteBalanceOnce();

  /**
   * White balance is adjusted repeatedly while images are acquired.
   */
  private native void setWhiteBalanceContinuous();

  // COLOR TRANSFORMATION
  //
  // Color transformation matrix RGB (3x3)
  //
  // R-to-R R-to-G R-to-B
  // G-to-R G-to-G G-to-B
  // B-to-R B-to-G B-to-B
  //

  /**
   * @return The minimum allowed value in color transformation matrix.
   */
  private native double getMinColorTransformationValue();

  /**
   * @return The maximum allowed value in color transformation matrix.
   */
  private native double getMaxColorTransformationValue();

  /**
   * @return Color transformation matrix for custom color transformation.
   */
  private native double[] getColorTransformationMatrix();

  /**
   * @param matrix
   *          Color transformation matrix for custom color transformation.
   */
  private native void setColorTransformationMatrix(double[] matrix);

  // LIGHT SOURCE

  /**
   * @return Light source preset. Possible values: "LightSourcePreset_Off", "LightSourcePreset_Daylight5000K", "LightSourcePreset_Daylight6500K" and "LightSourcePreset_Tungsten2800K
   */
  private native String getLightSourcePreset();

  /**
   * No color preset is set.
   */
  private native void setLightSourcePresetOff();

  /**
   * A color preset for image acquisition with daylight of 5000 K is set.
   */
  private native void setLightSourcePresetDaylight5000K();

  /**
   * A color preset for image acquisition with daylight of 6500 K is set.
   */
  private native void setLightSourcePresetDaylight6500K();

  /**
   * A color preset for image acquisition with tungsten incandescent light (2800 K) is set.
   */
  private native void setLightSourcePresetTungsten2800K();

  // RESET FACTORY DEFAULTS

  /**
   * Reset the camera settings to factory defaults.
   */
  private native void resetFactoryDefaults();

  // GRAB

  /**
   * Starts a grabbing process. Every captured frame will trigger a Java method call If a grabbing process is already running, it will fire an exception
   *
   * @param listenerClassName
   *          Name of the class containing the listener method. E.g "com/systelab/modulabgold/client/common/components/panels/videocaptura/basler/BaslerModel"
   * @param listenerMethodName
   *          Name of the callback method. E.g "onNewFrame"
   */
  private native void startGrabbing(String listenerClassName, String listenerMethodName);

  private native boolean isGrabbing();

  /*
  private native void nativeInit();

  private native boolean nativeStartConnection();

  private native boolean nativeEndConnection();

  private native void nativeAcquireNewFrame();

  private native void setExposureTime(int exposure);

  private native void setExposureAutoOnce();

  private native void nativeSetContinuousAET(boolean continuousAET);

  private native void nativeOneTimeAGC();

  private native void nativeSetContinuousAGC(boolean continuousAGC);

  private native void nativeSetGain(int gain);

  private native void nativeSetSharpness(int sharpness);

  private native void nativeSetGamma(int gamma);

  private native void nativeSetNegative(boolean negative);

  private native void nativeSetLightSource(int lightSource);

  private native void nativeSetRedLevel(int redLevel);

  private native void nativeSetGreenLevel(int greenLevel);

  private native void nativeSetBlueLevel(int blueLevel);

  private native void nativeOneTimeAWS();

  private native void nativeSetContinuousAWS(boolean continuousAWS);

  private native void nativeSetColorSaturation(int colorSaturation);

  private native boolean nativeRefreshProperties();

  */

}

package com.cameras.basler;

public class BaslerAcquisitionThread extends Thread
{

  private BaslerModel aBaslerModel;

  private boolean finished;
  private boolean acquireFrames;

  public BaslerAcquisitionThread(BaslerModel aBaslerModel)
  {
    super("Basler acquisition thread");
    this.aBaslerModel = aBaslerModel;
  }

  boolean doAcquireFrames()
  {
    return acquireFrames;
  }

  void setAcquireFrames(boolean acquireFrames)
  {
    this.acquireFrames = acquireFrames;
  }

  boolean isFinished()
  {
    return finished;
  }

  void setFinished(boolean finished)
  {
    this.finished = finished;
  }

  @Override
  public void run()
  {
    System.out.println("Running BaslerAcquisitionThread");
    while (!isFinished())
    {
      try
      {
        this.acquireNewFrame();
      }
      catch (Exception e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

    }
  }

  private void acquireNewFrame()
  {
    aBaslerModel.acquireNewFrame();
  }

}

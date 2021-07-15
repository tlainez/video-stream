package com.cameras.basler;

public class BaslerPropertiesObserverThread extends Thread
{

  private BaslerModel aBaslerModel;

  private boolean finished;

  public BaslerPropertiesObserverThread(BaslerModel aBaslerModel)
  {
    super();
    this.aBaslerModel = aBaslerModel;
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
    while (!isFinished())
    {
      aBaslerModel.refreshProperties();

      try
      {
        Thread.sleep(100);
      }
      catch (InterruptedException ie)
      {
        System.out.println(ie);
      }
    }
  }

}

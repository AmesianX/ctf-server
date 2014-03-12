/**
 * Author: Andrew Hood <andrewhood125@gmail.com>
 * Description: Store everything that a single lobby will need.
 * Copyright (c) 2014 Andrew Hood. All rights reserved.
 */

class Flag extends Locate
{

  // Flag range
  boolean isDropped;
  double west, east, north, south;
  Flag(double latitude, double longitude, double accuracy)
  {
    this.latitude = latitude;
    this.longitude = longitude;
    west = latitude - accuracy;
    east = latitude + accuracy;
    north = longitude + accuracy;
    south = longitude - accuracy;
    isDropped = true;
  } 

  public void updateLocation(Base base)
  {
    latitude = base.getLatitude();
    longitude = base.getLongitude();
  }
  
  public double getWest()
  {
    return west;
  }

  public double getEast()
  {
    return east;
  }

  public double getNorth()
  {
    return north;
  }

  public double getSouth()
  {
    return south;
  }

  public void setDropped(boolean bool)
  {
    isDropped = bool;
  }

  public boolean isDropped()
  {
    return isDropped;
  }
}

/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: algorithms/Stage1.java 2014-10-18 buixuan.
 * ******************************************************/
package algorithms;

import robotsimulator.Brain;
import characteristics.Parameters;
import characteristics.Parameters.Direction;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;

import java.util.ArrayList;

public class Stage2 extends Brain {
  //---PARAMETERS---//
  private static final double HEADINGPRECISION = 0.001;
  private static final double ANGLEPRECISION = 0.1;
  private static final int ROCKY = 0x1EADDA;
  private static final int CARREFOUR = 0x5EC0;
  private static final int DARTY = 0x333;
  private static final int UNDEFINED = 0xBADC0DE;

  //---VARIABLES---//
  private boolean turnNorthTask,turnLeftTask;
  private double endTaskDirection;
  private double distLateral, distTop;
  private boolean isMoving;
  private int whoAmI;

  //---CONSTRUCTORS---//
  public Stage2() { super(); }

  //---ABSTRACT-METHODS-IMPLEMENTATION---//
  public void activate() {
    //ODOMETRY CODE
    whoAmI = ROCKY;
    for (IRadarResult o: detectRadar())
      if (isSameDirection(o.getObjectDirection(),Parameters.NORTH)) whoAmI=UNDEFINED;
    if (whoAmI == ROCKY){
      distLateral = 0;
      distTop=0;
    }

    //INIT
    turnNorthTask=true;
    turnLeftTask=false;
    isMoving=false;
  }
  public void step() {
    stepTurn(Direction.LEFT);
  }
  private void myMove(){
    isMoving=true;
    move();
  }
  private boolean isHeading(double dir){
    return Math.abs(Math.sin(getHeading()-dir))<HEADINGPRECISION;
  }
  private boolean isSameDirection(double dir1, double dir2){
    return Math.abs(dir1-dir2)<ANGLEPRECISION;
  }
}
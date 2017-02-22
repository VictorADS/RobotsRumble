/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: algorithms/Stage1.java 2014-10-18 buixuan.
 * ******************************************************/
package algorithms;

import robotsimulator.Brain;
import characteristics.Parameters;
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
    //ODOMETRY CODE
    if (isMoving      &&  whoAmI == ROCKY             ){
      isMoving=false;
    }
    //DEBUG MESSAGE
    if (whoAmI == ROCKY) {
    	if(isSameDirection(getHeading(),Parameters.NORTH)){
    		distTop ++;
    	}
    	if(isSameDirection(getHeading(),Parameters.EAST))
   	        distLateral++;
    	sendLogMessage("J'ai ca "+String.format("%.2f",distLateral)+" et "+String.format("%.2f",distTop)+" Head "+String.format("%.2f",getHeading())+"\n N "+String.format("%.2f",Parameters.NORTH)+" L "+String.format("%.2f",Parameters.EAST ));
    }
    //AUTOMATON
    
    /*** Permet au robot de se positioner vers son NORD ***/
    if (turnNorthTask && isHeading(Parameters.NORTH)) {
      turnNorthTask=false;
      myMove();
      return;
    }
    /***  Tant que le robot n'est pas bien positionne on tourne a droite jusqu'a atteindre le NORD ***/
    if (turnNorthTask && !isHeading(Parameters.NORTH)) {
      stepTurn(Parameters.Direction.RIGHT);
      //sendLogMessage("Initial TeamB position. Heading North!");
      return;
    }
    
    /*** Permet au robot de se positioner vers sa GAUCHE ***/
    if (turnLeftTask && isHeading(endTaskDirection)) {
      turnLeftTask=false;
      myMove();
      //sendLogMessage("Moving a head. Waza!");
      return;
    }
    /***  Tant que le robot n'est pas bien positionne on tourne a gauche jusqu'a atteindre la valeur de LEFTTURNFULLANGLE  ***/
    if (turnLeftTask && !isHeading(endTaskDirection)) {
      stepTurn(Parameters.Direction.LEFT);
      //sendLogMessage("Iceberg at 12 o'clock. Heading 9!");
      return;
    }
    
    /*** Si le robot n'est pas en mode tourner et qu'il detecte un wall alors tourne a gauche ***/
    if (!turnNorthTask && !turnLeftTask && detectFront().getObjectType()==IFrontSensorResult.Types.WALL) {
      turnLeftTask=true;
      endTaskDirection=getHeading()+Parameters.LEFTTURNFULLANGLE;
      stepTurn(Parameters.Direction.LEFT);
      //sendLogMessage("Iceberg at 12 o'clock. Heading 9!");
      return;
    }
    /*** Si le robot n'est pas en mode tourner et qu'il detecte un wall alors avance***/
    if (!turnNorthTask && !turnLeftTask && detectFront().getObjectType()!=IFrontSensorResult.Types.WALL) {
      myMove(); //And what to do when blind blocked?
      //sendLogMessage("Moving a head. Waza!");
      return;
    }
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
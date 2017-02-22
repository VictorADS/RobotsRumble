/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: algorithms/Stage1.java 2014-10-18 buixuan.
 * ******************************************************/
package algorithms;

import robotsimulator.Brain;
import characteristics.IFrontSensorResult.Types;
import characteristics.Parameters;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;

import java.util.ArrayList;

public class FightBrain extends Brain {
	// ---PARAMETERS---//
	private static final double HEADINGPRECISION = 0.001;
	private static final double ANGLEPRECISION = 0.1;
	private static final int ROCKY = 0x1EADDA;
	private static final int CARREFOUR = 0x5EC0;
	private static final int DARTY = 0x333;
	private static final int UNDEFINED = 0xBADC0DE;

	// ---VARIABLES---//
	private boolean turnNorthTask, turnLeftTask;
	private double endTaskDirection;
	private double distLateral, distTop;
	private boolean isMoving;
	private int whoAmI;
	private boolean doNotShoot;
	// ---CONSTRUCTORS---//
	public FightBrain() {
		super();
	}

	// ---ABSTRACT-METHODS-IMPLEMENTATION---//
	public void activate() {
		// ODOMETRY CODE
		whoAmI = ROCKY;
		for (IRadarResult o : detectRadar())
			if (isSameDirection(o.getObjectDirection(), Parameters.NORTH))
				whoAmI = UNDEFINED;
		if (whoAmI == ROCKY) {
			distLateral = 0;
			distTop = 0;
		}

		// INIT
		turnNorthTask = false;
		turnLeftTask = false;
		isMoving = false;
	}

	public void step() {
		 ArrayList<IRadarResult> radarResults;
		 if(getHealth() <= 0)
			 return;
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
      return;
    }
    
    /*** Permet au robot de se positioner vers sa GAUCHE ***/
    if (turnLeftTask && isHeading(endTaskDirection)) {
      turnLeftTask=false;
      myMove();
      return;
    }
    /***  Tant que le robot n'est pas bien positionne on tourne a gauche jusqu'a atteindre la valeur de LEFTTURNFULLANGLE  ***/
    if (turnLeftTask && !isHeading(endTaskDirection)) {
      stepTurn(Parameters.Direction.LEFT);
      return;
    }
    
    /*** Si le robot n'est pas en mode tourner et qu'il detecte un wall alors tourne a gauche ***/
    if (!turnNorthTask && !turnLeftTask && detectFront().getObjectType()==IFrontSensorResult.Types.WALL) {
	      turnLeftTask=true;
	      endTaskDirection=getHeading()+Parameters.LEFTTURNFULLANGLE;
	      stepTurn(Parameters.Direction.LEFT);
	      return;
    }
    
    /** MA PARTIE **/
    /*** Si le robot n'est pas en mode tourner et qu'il detecte un allie qui tourne alors il ya comportement***/
    if (!turnNorthTask && !turnLeftTask && detectFront().getObjectType()==IFrontSensorResult.Types.TeamMainBot) {
    	radarResults = detectRadar();
      for (IRadarResult r : radarResults) {
          if (r.getObjectType()==IRadarResult.Types.TeamMainBot) {
        	  /*** Il faut tourner a gauche car le mec devant tourne ***/
        	  if(r.getObjectDirection() != getHeading()){
	              turnLeftTask=true;
	              endTaskDirection=getHeading()+Parameters.LEFTTURNFULLANGLE;
	              stepTurn(Parameters.Direction.LEFT);
	              return;
        	  }
          }
      }
      myMove(); //And what to do when blind blocked?
      return;
    }
    if (!turnNorthTask && !turnLeftTask && detectFront().getObjectType()==IFrontSensorResult.Types.Wreck) {
        turnLeftTask=true;
        endTaskDirection=getHeading()+Parameters.LEFTTURNFULLANGLE;
        stepTurn(Parameters.Direction.LEFT);
        return;
    }
    if (!turnNorthTask && !turnLeftTask) {
        radarResults = detectRadar();
        int enemyFighters=0,enemyPatrols=0;
        double enemyDirection=0;
        doNotShoot = false;
        for (IRadarResult r : radarResults) {
        	/** Focus le Main **/
          if (r.getObjectType()==IRadarResult.Types.OpponentMainBot) {
            enemyFighters++;
            enemyDirection=r.getObjectDirection();
            continue;
          }
          /** Au cas où il ya un secondary **/
          if (r.getObjectType()==IRadarResult.Types.OpponentSecondaryBot) {
            if (enemyFighters==0) enemyDirection=r.getObjectDirection();
            enemyPatrols++;
          }
          /** Ne pas tirer sur friends **/
          if(/*r.getObjectType()==IRadarResult.Types.TeamMainBot || */r.getObjectType() == IRadarResult.Types.TeamSecondaryBot){
        	  if(isInFrontOfMe(r.getObjectDirection()))
        		  doNotShoot = true;
          }
        }
        if(enemyFighters+enemyPatrols == 0)
        	myMove();
        else{
        	myMove(enemyDirection);
        }
    }  
    }

	private void myMove() {
		isMoving = !isMoving;
		if(isMoving)
			move();
		else if(!doNotShoot)
			fire(getHeading());
	}
	private void myMove(double enemyDirection) {
		isMoving = !isMoving;
		if(isMoving)
			move();
		else if(!doNotShoot)
			fire(enemyDirection);
	}

	private boolean isHeading(double dir) {
		return Math.abs(Math.sin(getHeading() - dir)) < HEADINGPRECISION;
	}

	private boolean isSameDirection(double dir1, double dir2) {
		return Math.abs(dir1 - dir2) < ANGLEPRECISION;
	}
	private boolean isInFrontOfMe(Double enemy) {
		double heading = getHeading();
		double left = 0.2 * Math.PI;
		double right = -0.2 * Math.PI;
		System.out.println("Jai heading "+heading+" et en"+enemy+" avec "+(enemy >= heading + left && enemy <= heading + right));
		return enemy <= heading + left && enemy >= heading + right;
	}
}
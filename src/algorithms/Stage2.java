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

import java.awt.Point;
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
  private Point myCoords;
  private boolean devant;
  private int whoAmI;
  private static int cpt = 0;
  //---CONSTRUCTORS---//
  public Stage2() { super(); }

  //---ABSTRACT-METHODS-IMPLEMENTATION---//
  public void activate() {
    //ODOMETRY CODE
	whoAmI = cpt++ % 3;
	switch(whoAmI){
	case 0:
		myCoords = new Point((int)Parameters.teamAMainBot1InitX,(int)Parameters.teamAMainBot1InitY);
		break;
	case 1:
		myCoords = new Point((int)Parameters.teamAMainBot2InitX,(int)Parameters.teamAMainBot2InitY);
		break;
	case 2:
		myCoords = new Point((int)Parameters.teamAMainBot3InitX,(int)Parameters.teamAMainBot3InitY);
		break;
	}
	devant = true;
  }
  public void step() {
	  if(whoAmI < 1)
		  return;
    sendLogMessage("position ("+myCoords.x+", "+(int)myCoords.y+"). Avec un heading De "+getHeading());
	  if (detectFront().getObjectType()==IFrontSensorResult.Types.WALL) {
		  devant = false;
		  if(whoAmI== 1)
			  stepTurn(Direction.LEFT);
		  else
			  stepTurn(Direction.RIGHT);
		  return;
	  }
	  if(devant)
		  MyMove();
	  else
		  MyMoveBack();
	  
  }
	private void MyMove(){
		myCoords.setLocation(myCoords.getX() + Parameters.teamAMainBotSpeed * Math.cos(getHeading()), myCoords.getY() + Parameters.teamAMainBotSpeed * Math.sin(getHeading()));
		move();
	}
	private void MyMoveBack(){
		myCoords.setLocation(myCoords.getX() - Parameters.teamAMainBotSpeed * Math.cos(getHeading()), myCoords.getY() - Parameters.teamAMainBotSpeed * Math.sin(getHeading()));
		moveBack();
	}
  private boolean isHeading(double dir){
    return Math.abs(Math.sin(getHeading()-dir))<HEADINGPRECISION;
  }
  private boolean isSameDirection(double dir1, double dir2){
    return Math.abs(dir1-dir2)<ANGLEPRECISION;
  }
}
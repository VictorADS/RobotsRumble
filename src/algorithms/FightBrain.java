/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: algorithms/Stage1.java 2014-10-18 buixuan.
 * ******************************************************/
package algorithms;

import robotsimulator.Brain;
import characteristics.IFrontSensorResult.Types;
import characteristics.Parameters.Direction;
import characteristics.Parameters;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;

import java.util.ArrayList;
import java.util.Random;

import javax.jws.soap.SOAPBinding.ParameterStyle;
import javax.swing.text.AbstractDocument.LeafElement;

public class FightBrain extends Brain {
	// ---PARAMETERS---//
	private static final double HEADINGPRECISION = 0.001;
	private static final double ANGLEPRECISION = 0.1;
	private static final int ROCKY = 0x1EADDA;
	private static final int CARREFOUR = 0x5EC0;
	private static final int DARTY = 0x333;
	private static final int UNDEFINED = 0xBADC0DE;

	// ---VARIABLES---//
	private boolean turnNorthTask, turnLeftTask, turnRightTask, dodgeTask, moveBackTask;
	private double endTaskDirection;
	private double distLateral, distTop;
	private boolean isMoving;
	private int whoAmI;
	private boolean doNotShoot;
	private int nbTurns = 0;
	private static Random rand = new Random(); 
	// ---CONSTRUCTORS---//
	public FightBrain() {
		super();
	}
	private static int  lol = 0;
	// ---ABSTRACT-METHODS-IMPLEMENTATION---//
	public void activate() {
		// ODOMETRY CODE
		whoAmI = lol++;

		// INIT
		turnNorthTask = false;
		turnLeftTask = false;
		turnRightTask = false;
		moveBackTask = false;
		dodgeTask = false;
		isMoving = false;
	}

	public void step() {
		ArrayList<IRadarResult> radarResults;
		if (getHealth() <= 0)
			return;
		if (whoAmI % 3 == 2) {
			if (isSameDirection(getHeading(), Parameters.NORTH)) {
				distTop++;
			}
			if (isSameDirection(getHeading(), Parameters.EAST))
				distLateral++;
			sendLogMessage("J'ai ca " + String.format("%.2f", distLateral) + " et " + String.format("%.2f", distTop)
					+ " Head " + String.format("%.2f", getHeading()) + "\n N " + String.format("%.2f", Parameters.NORTH)
					+ " L " + String.format("%.2f", Parameters.EAST));
		}
    //AUTOMATON
		/*** Permet de reculer lorsque trop rpes ***/
		if(moveBackTask && nbTurns == 0){
			moveBackTask = false;
			stepTurn(Math.random() > 0.7 ? Direction.RIGHT : Direction.LEFT);
			System.out.println("Toz");
			return;
		}
		if (moveBackTask && nbTurns > 0) {
			moveBack();
			nbTurns--;
	        return;
		}
		/*** Permet au robot de se positioner vers son NORD ***/
		if (dodgeTask && nbTurns == 0) {
			dodgeTask = false;
			myMove();
			return;
		}
		/***
		 * Tant que le robot n'est pas bien positionne on tourne a droite
		 * jusqu'a atteindre le NORD
		 ***/
		if (dodgeTask && nbTurns > 0) {
	        stepTurn(Direction.LEFT);
	        nbTurns--;
	        return;
		}

		/*** Permet au robot de se positioner vers son NORD ***/
		if (turnNorthTask && isHeading(Parameters.NORTH)) {
			turnNorthTask = false;
			myMove();
			return;
		}
		/***
		 * Tant que le robot n'est pas bien positionne on tourne a droite
		 * jusqu'a atteindre le NORD
		 ***/
		if (turnNorthTask && !isHeading(Parameters.NORTH)) {
			stepTurn(Parameters.Direction.RIGHT);
			return;
		}

		/***
		 * Si le robot n'est pas en mode tourner et qu'il detecte un wall alors
		 * tourne a gauche
		 ***/
		if (!turnNorthTask && !turnLeftTask && (detectFront().getObjectType() == IFrontSensorResult.Types.WALL ||  detectFront().getObjectType() == IFrontSensorResult.Types.Wreck)) {
			dodgeLeft();
			return;
		}

		/** MA PARTIE **/
		/***
		 * Si le robot n'est pas en mode tourner et qu'il detecte un allie qui
		 * tourne alors il ya comportement
		 ***/
		if (!turnNorthTask && !turnLeftTask && detectFront().getObjectType() == IFrontSensorResult.Types.TeamMainBot) {
			radarResults = detectRadar();
			for (IRadarResult r : radarResults) {
				if (r.getObjectType() == IRadarResult.Types.TeamMainBot) {
					/*** Il faut tourner a gauche car le mec devant tourne ***/
					if (r.getObjectDirection() != getHeading()) {
						dodgeLeft();
						return;
					}
				}
			}
			myMove(); // And what to do when blind blocked?
			return;
		}

		if (!turnNorthTask && !turnLeftTask) {
			radarResults = detectRadar();
			int enemyFighters = 0, enemyPatrols = 0;
			double enemyDirection = 0;
			doNotShoot = false;
			for (IRadarResult r : radarResults) {
				/** Focus le Main **/
				if (r.getObjectType() == IRadarResult.Types.OpponentMainBot) {
					enemyFighters++;
					enemyDirection = r.getObjectDirection();
				}
				/** Au cas ou il ya un secondary **/
				if (r.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
					if (enemyFighters == 0)
						enemyDirection = r.getObjectDirection();
					enemyPatrols++;
				}
				/** Ne pas tirer sur friends **/
				if (r.getObjectType() == IRadarResult.Types.TeamMainBot
						|| r.getObjectType() == IRadarResult.Types.TeamSecondaryBot) {
					if (isInFrontOfMe(r.getObjectDirection()) && enemyFighters + enemyPatrols == 0) {
						doNotShoot = true;
						if (r.getObjectDistance() <= r.getObjectRadius() + Parameters.teamAMainBotRadius + 40) {
							dodgeLeft();
							return;
						}
					}

				}
				/** Reculer si trop proche **/
				if(r.getObjectType() == IRadarResult.Types.TeamMainBot || r.getObjectType() == IRadarResult.Types.TeamSecondaryBot || r.getObjectType() == IRadarResult.Types.Wreck){
					if(r.getObjectDistance() <= r.getObjectRadius() + Parameters.teamAMainBotRadius + 10 && !dodgeTask){
						moveBackTast();
						return;
					}
				}
			}

			/*** Comporte de base lorsque dennemi detecte ***/
			if (enemyFighters + enemyPatrols > 0) {
				attack(enemyDirection);
				return;
			}
		}
		
		/*** DEFAULT COMPORTEMENT ***/
		double randDouble = Math.random();
		if(randDouble <= 0.60){
			move();
			return;
		}
		if(randDouble <= 0.80){
			stepTurn(Direction.LEFT);
			return;
		}
		if(randDouble <= 1.00 ){
			stepTurn(Direction.RIGHT);
			return;
		}
	}
	private void dodgeLeft(){
		dodgeTask = true;
		nbTurns = rand.nextInt(20);
	}
	private void moveBackTast(){
		moveBack();
		moveBackTask = true;
		nbTurns = rand.nextInt(20);
	}
	private void myMove() {
		isMoving = !isMoving;
		if(isMoving)
			move();
		else if(!doNotShoot)
			fire(getHeading());
	}
	private void attack(double enemyDirection) {
		isMoving = !isMoving;
		if(isMoving){
				moveBack();
		}
		else if(!doNotShoot){
			fire(enemyDirection);
		}
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
		double left = 0.15 * Math.PI;
		double right = -0.15 * Math.PI;
		boolean res = enemy <= (heading + left) % (2*Math.PI) && enemy >= (heading + right) % (2*Math.PI);
		return res;
	}
}
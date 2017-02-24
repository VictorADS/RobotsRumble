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
import java.util.Optional;
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
	enum BOUSSOLE { DEVANT, DERRIERE, GAUCHE, DROITE};
	// ---VARIABLES---//
	private boolean dodgeLeftTask, dodgeRightTask, dodgeTask, moveFrontTask, moveBackTask;
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
		moveFrontTask = false;
		moveBackTask = false;
		dodgeTask = false;
		dodgeLeftTask = false;
		dodgeRightTask = false;
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
			dodgeObstacle();
		}
		if (moveBackTask && nbTurns > 0) {
			moveBack();
			nbTurns--;
	        return;
		}
		
		/*** Permet de reculer lorsque trop rpes ***/
		if(moveFrontTask && nbTurns == 0){
			moveFrontTask = false;
		}
		if (moveFrontTask && nbTurns > 0) {
			move();
			nbTurns--;
	        return;
		}
		/*** Permet au robot de se positioner vers son NORD ***/
		if (dodgeTask && nbTurns == 0) {
			dodgeTask = false;
			dodgeLeftTask = false;
			dodgeRightTask = false;
		}
		/***
		 * Tant que le robot n'est pas bien positionne on tourne a droite
		 * jusqu'a atteindre le NORD
		 ***/
		if (dodgeTask && nbTurns > 0) {
			if(dodgeLeftTask)
				stepTurn(Direction.LEFT);
			else
				stepTurn(Direction.RIGHT);
	        nbTurns--;
	        return;
		}

		/***
		 * Si le robot n'est pas en mode tourner et qu'il detecte un wall alors
		 * tourne a gauche
		 ***/
		if ((detectFront().getObjectType() == IFrontSensorResult.Types.WALL ||  detectFront().getObjectType() == IFrontSensorResult.Types.Wreck)) {
			for (IRadarResult r : detectRadar()) {
				if(r.getObjectType() == IRadarResult.Types.Wreck && r.getObjectDistance() <= r.getObjectRadius() + Parameters.teamAMainBotRadius + 80 ){
					dodgeObstacle(r.getObjectDirection(), r.getObjectDistance());
					System.out.println("Je detecte wreck");
					return;
				}
			}
			System.out.println("Je detecte un mur");
			dodgeObstacle();
			return;
		}

		if (!dodgeTask && !moveBackTask) {
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
						System.out.println("Je ne dois pas tire");
						doNotShoot = true;
					}
					if (r.getObjectDistance() <= r.getObjectRadius() + Parameters.teamAMainBotRadius + 80) {
						dodgeObstacle(r.getObjectDirection(), r.getObjectDistance());
						System.out.println("Je dois esquive un allie");
						return;
					}
				}
				/** Reculer si trop proche **/
				if(r.getObjectType() == IRadarResult.Types.TeamMainBot || r.getObjectType() == IRadarResult.Types.TeamSecondaryBot || r.getObjectType() == IRadarResult.Types.Wreck){
					if(r.getObjectDistance() <= r.getObjectRadius() + Parameters.teamAMainBotRadius + 20 && !dodgeTask){
						System.out.println("Je dois recule");
						moveBackTast(r.getObjectDirection());
						return;
					}
				}
			}

			/*** Comporte de base lorsque dennemi detecte ***/
			if (enemyFighters + enemyPatrols > 0) {
				System.out.println("Jattaque");
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
	
	
	private void dodgeObstacle(){
		dodgeTask = true;
		if(Math.random() > 0.5){
			dodgeLeftTask = true;
		}else{
			dodgeRightTask = true;
		}
		nbTurns = rand.nextInt(40);
	}
	private void dodgeObstacle(double pos, double distance){
		dodgeTask = true;
		if(isADroite(pos) && isDevant(pos)){
			dodgeLeftTask = true;
			nbTurns = rand.nextInt(40);
			return;
		}
		if(isAGauche(pos) && isDevant(pos)){
			dodgeRightTask = true;
			nbTurns = rand.nextInt(40);
			return;
		}
		if(isDevant(pos)){
			moveBackTask = true;
			nbTurns = rand.nextInt(40);
			return;
		}
		if(isDerriere(pos)){
			moveFrontTask = true;
			nbTurns = rand.nextInt(40);
			return;
		}
	}
	private void moveBackTast(double pos){
		if(isDerriere(pos)){
			System.out.println("Je vais devant");
			moveFrontTask = true;
		}else{
			System.out.println("Je vais derriere");
			moveBackTask = true;
		}
		nbTurns = rand.nextInt(40);
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
			if(isDerriere(enemyDirection))
				move();
			else
				moveBack();
				return;
		}
		else if(!doNotShoot){
			fire(enemyDirection);
			return;
		}
		if(Math.random() >= 0.5) {
			stepTurn(Direction.LEFT);
		}else{
			stepTurn(Direction.RIGHT);
		}
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
	private boolean isDevant(double pos){
		double heading = getHeading();
		double left = 0.5 * Math.PI;
		System.out.println("POs = "+pos+" HEADING "+heading+" Left "+((heading + left) % (2*Math.PI))+" right "+((heading - left) % (2*Math.PI))+" res "+(pos <= (heading + left) % (2*Math.PI) && pos >= (heading - left) % (2*Math.PI)));
		return pos <= (heading + left) % (2*Math.PI) && pos >= (heading - left) % (2*Math.PI);
	}
	
	private boolean isDerriere(double pos){
		return !isDevant(pos);
	}
	
	private boolean isAGauche(double pos){
		double heading = getHeading();
		double left =Math.PI;
		System.out.println("POs = "+pos+" HEADING "+heading+" Right "+(heading)+" Left "+((heading - left))+" resultat = "+(pos <= heading && pos >= (heading - left)));
		return pos <= heading % (2*Math.PI) && pos >= (heading - left) % (2*Math.PI);
	}
	
	private boolean isADroite(double pos){
		return !isAGauche(pos);
	}
}
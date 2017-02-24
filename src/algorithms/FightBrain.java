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

import java.awt.Point;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;

import javax.jws.soap.SOAPBinding.ParameterStyle;
import javax.swing.text.AbstractDocument.LeafElement;

public class FightBrain extends Brain {
	// ---PARAMETERS---//
	private static final double HEADINGPRECISION = 0.001;
	private static final double ANGLEPRECISION = 0.1;
	// ---VARIABLES---//
	private boolean repositioningTask, dodgeLeftTask, dodgeRightTask, dodgeTask, moveFrontTask, moveBackTask;
	private double distLateral, distTop;
	private double endRepositioningDirection;
	private boolean isMoving;
	private int whoAmI;
	private Point  myCoords;
	private boolean doNotShoot;
	private int nbTurns = 0;
	private boolean shouldMove;
	private static Random rand = new Random(); 
	// ---CONSTRUCTORS---//
	public FightBrain() {
		super();
	}
	private static int  lol = 0;
	// ---ABSTRACT-METHODS-IMPLEMENTATION---//
	public void activate() {
		// ODOMETRY CODE
		whoAmI = lol++ % 3;
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
		// INIT
		moveFrontTask = false;
		moveBackTask = false;
		dodgeTask = false;
		dodgeLeftTask = false;
		repositioningTask= false;
		dodgeRightTask = false;
		isMoving = false;
		shouldMove = false;
	}

	public void step() {
		ArrayList<IRadarResult> radarResults;
		if (getHealth() <= 0)
			return;
		if (isMoving) {
			myCoords.setLocation(myCoords.getX() + Parameters.teamAMainBotSpeed * Math.cos(getHeading()), myCoords.getY() + Parameters.teamAMainBotSpeed * Math.sin(getHeading()));
			isMoving = false;
		}
		sendLogMessage("position ("+myCoords.x+", "+(int)myCoords.y+"). Avec un heading De "+getHeading());
		if(whoAmI == 1){ // Leader est whoAmI = 1
			broadcast(whoAmI+"-"+myCoords.x+"-"+myCoords.y);
			for(String s : fetchAllMessages()){
				System.out.println(s+" et "+s.startsWith("Je suis"));
				if(s.startsWith("Je suis"))
					return;
			}
		}
		//AUTOMATON
		/*** Permet de se positioner pour se rapproche du leader ***/
		if(repositioningTask && isHeading(endRepositioningDirection)){
			isMoving = true;
			move();
			repositioningTask = false;
			return;
		}
		if(repositioningTask && !isHeading(endRepositioningDirection)){
			stepTurn(Parameters.Direction.RIGHT);
			return;
		}
		/*** Permet de reculer lorsque trop rpes ***/
		if(moveBackTask && nbTurns == 0){
			moveBackTask = false;
			dodgeObstacle();
			return;
		}
		if (moveBackTask && nbTurns > 0) {
			moveBack();
			nbTurns--;
	        return;
		}
		
		/*** Permet de reculer lorsque trop rpes ***/
		if(moveFrontTask && nbTurns == 0){
			moveFrontTask = false;
			return;
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
			return;
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
				if(r.getObjectType() == IRadarResult.Types.Wreck && r.getObjectDistance() <= r.getObjectRadius() + Parameters.teamAMainBotRadius + 50){
					dodgeObstacle(r.getObjectDirection(), r.getObjectDistance());
					return;
				}
			}
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
						doNotShoot = true;
					}
					
					if (r.getObjectDistance() <= r.getObjectRadius() + Parameters.teamAMainBotRadius + 50 &&  (enemyFighters+enemyPatrols) == 0) {
						dodgeObstacle(r.getObjectDirection(), r.getObjectDistance());
						return;
					}
				}
				
				if(r.getObjectType() == IRadarResult.Types.Wreck ){
					if (r.getObjectDistance() <= r.getObjectRadius() + Parameters.teamAMainBotRadius + 50 &&  (enemyFighters+enemyPatrols) == 0) {
						dodgeObstacle(r.getObjectDirection(), r.getObjectDistance());
						return;
					}
				}
				/** Reculer si trop proche **/
				if(r.getObjectType() == IRadarResult.Types.TeamMainBot || r.getObjectType() == IRadarResult.Types.TeamSecondaryBot || r.getObjectType() == IRadarResult.Types.Wreck){
					if(r.getObjectDistance() <= r.getObjectRadius() + Parameters.teamAMainBotRadius + 20 && !dodgeTask && (enemyFighters+enemyPatrols) == 0){
						moveBackTast(r.getObjectDirection());
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
		// Ici on essaye de rester close sinon random
		if(whoAmI != 1){
			for(String s : fetchAllMessages()){
				String tab[] = s.split("-");
				if(tab.length <= 1)
					continue;
				Point leaderCoord = new Point(Integer.parseInt(tab[1]), Integer.parseInt(tab[2]));
				if(leaderCoord.distance(myCoords) >= 500){
					repositioningTask = true;
					broadcast("Je suis trop loin attend moi");
					approximate(leaderCoord);
					return;
				}
			}
		}
		repositioningTask = false;
		moveRandom();
	}
	


	private void moveRandom(){
		/*** DEFAULT COMPORTEMENT ***/
		double randDouble = Math.random();
		isMoving = false;
		if(randDouble <= 0.60){
			isMoving = true;
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
		isMoving = false;
		if(Math.random() > 0.5){
			dodgeLeftTask = true;
		}else{
			dodgeRightTask = true;
		}
		nbTurns = rand.nextInt(40);
	}
	private void dodgeObstacle(double pos, double distance){
		dodgeTask = true;
		isMoving = false;
		if(isADroite(pos) && isDevant(pos)){
			dodgeLeftTask = true;
		}else{
			if(isAGauche(pos) && isDevant(pos)){
			dodgeRightTask = true;
			}else{
				isMoving = true;
				if(isDevant(pos)){
					moveBackTask = true;
				}else{
					moveFrontTask = true;
				}
			}
		}
		nbTurns = rand.nextInt(40);
	}
	private void moveBackTast(double pos){
		isMoving = true;
		if(isDerriere(pos)){
			moveFrontTask = true;
		}else{
			moveBackTask = true;
		}
		nbTurns = rand.nextInt(40);
	}

	private void attack(double enemyDirection) {
		shouldMove = !shouldMove;
		isMoving = false;
		if(shouldMove){
		//	isMoving = true;
			if(isDerriere(enemyDirection)){
				move();
				return;
			}else{
				moveBack();
			}
		}
		else if(!doNotShoot){
			fire(enemyDirection);
			return;
		}
	}

	private boolean isHeading(double dir) {
		return Math.abs(Math.sin(getHeading() - dir)) < HEADINGPRECISION;
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
		if(heading < 0 )
			heading = (heading + 2 * Math.PI) % (2 * Math.PI);
		if(pos < 0)
			pos = (pos + 2 * Math.PI) % (2 * Math.PI);
		
		double leftBorn = (heading + left) % (2*Math.PI);
		double rightBorn = (heading - left) % (2*Math.PI);
		if(leftBorn < 0)
			leftBorn = (leftBorn + 2 * Math.PI) % (2 * Math.PI);
		if(rightBorn < 0)
			rightBorn = (rightBorn + 2 * Math.PI) % (2 * Math.PI);
		if(heading - left > 0 && heading + left < 2 * Math.PI){
			return pos <= leftBorn  && pos >= rightBorn;
		}else{
				return pos >= rightBorn || pos <= leftBorn;
		}
	}
	
	private boolean isDerriere(double pos){
		return !isDevant(pos);
	}
	
	private boolean isAGauche(double pos){
		double heading = getHeading();
		if(heading < 0 )
			heading = heading + 2 * Math.PI;
		if(pos < 0)
			pos = (pos + 2 * Math.PI) % (2 * Math.PI);
		double left = Math.PI;
		double leftBorn = heading % (2 * Math.PI); // Heading actuel
		double rightBorn = (heading - left) % (2 * Math.PI); // Heading - PI

		if(leftBorn < 0)
			leftBorn = (leftBorn + 2 * Math.PI) % (2 * Math.PI);
		if(rightBorn < 0)
			rightBorn = (rightBorn + 2 * Math.PI) % (2 * Math.PI);
		
		if(heading - Math.PI > 0){ // Cas dans les bornes
			return pos <= leftBorn  && pos >= rightBorn ;
		}else{
			return pos >= rightBorn || pos <= leftBorn;
		}
				
	}
	
	private boolean isADroite(double pos){
		return !isAGauche(pos);
	}
	
	
	
	private void approximate(Point leaderCoord) {
		isMoving = false;
		if(myCoords.x >= leaderCoord.x - 250  && myCoords.x <= leaderCoord.x + 250){
			if(myCoords.y >= leaderCoord.y - 250 && myCoords.y <= leaderCoord.y + 250){
				moveRandom(); // Cas random au cas ou
			}else{//Sinon il faut se rapproche du Y
				if(myCoords.y > leaderCoord.y){
					monter();
				}else{
					descendre();
				}
			}
		}else{ // Sinon rapproche du X
			if(myCoords.x > leaderCoord.x ){
				gauche();
			}else{
				droite();
			}
		}
	}
	/**** COMMANDE TO MOVE ***/
	private void monter(){
		endRepositioningDirection = Parameters.SOUTH;
	}
	private void descendre(){
		endRepositioningDirection = Parameters.NORTH;
	}
	private void gauche(){
		endRepositioningDirection = Parameters.EAST;
	}
	private void droite(){
		endRepositioningDirection = Parameters.WEST;
	}
}
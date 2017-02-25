/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: algorithms/Stage1.java 2014-10-18 buixuan.
 * ******************************************************/
package algorithms;

import robotsimulator.Brain;
import characteristics.Parameters.Direction;
import characteristics.Parameters;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Random;


public class FightBrain extends Brain {
	// ---PARAMETERS---//
	private static final double HEADINGPRECISION = 0.001;
	private static final double ANGLEPRECISION = 0.1;
	private static final int DISTANCE_TO_LEADER = 400;
	// ---VARIABLES---//
	private boolean repositioningTask, waitTask, dodgeLeftTask, dodgeRightTask, dodgeTask, moveFrontTask, moveBackTask;
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
		waitTask = false;
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
		int enemyFighters, enemyPatrols;
		double enemyDirection;
		ArrayList<IRadarResult> radarResults;
		if (getHealth() <= 0)
			return;
		sendLogMessage("position ("+myCoords.x+", "+(int)myCoords.y+"). Avec un heading De "+getHeading());
		if(whoAmI == 1){ // Leader est whoAmI = 1
			broadcast(whoAmI+"-"+myCoords.x+"-"+myCoords.y);
			waitTask = false;
			for(String s : fetchAllMessages()){
				if(s.startsWith("Je suis"))
					waitTask = true;
			}
		}
		//AUTOMATON
		if(waitTask){
			enemyFighters = 0;
			enemyPatrols = 0;
			enemyDirection = 0;
			doNotShoot = false;
			for (IRadarResult r : detectRadar()) {
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
			}
			if (enemyFighters + enemyPatrols > 0) {
				attack(enemyDirection);
			}
			return;
		}
		/*** Permet de se positioner pour se rapproche du leader ***/
		if(repositioningTask){
			broadcast("Je suis trop loin attend moi");
			if(isHeading(endRepositioningDirection)){
				isMoving = true;
				MyMove();
				repositioningTask = false;
				return;
			}else{
				double heading = getHeading() % (2 * Math.PI);
				if(heading < 0 )
					heading += 2 * Math.PI;
				if(heading - endRepositioningDirection < Math.PI && heading -endRepositioningDirection >= 0)
					stepTurn(Direction.LEFT);
				else
					stepTurn(Direction.RIGHT);
				return;
			}
			
		}
		/*** Permet de reculer lorsque trop rpes ***/
		if(moveBackTask && nbTurns == 0){
			moveBackTask = false;
			dodgeObstacle();
			return;
		}
		if (moveBackTask && nbTurns > 0) {
			MyMoveBack();
			nbTurns--;
	        return;
		}
		
		/*** Permet de reculer lorsque trop rpes ***/
		if(moveFrontTask && nbTurns == 0){
			moveFrontTask = false;
			return;
		}
		if (moveFrontTask && nbTurns > 0) {
			MyMove();
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


		if (!dodgeTask && !moveBackTask) {
			radarResults = detectRadar();
			enemyFighters = 0;
			enemyPatrols = 0;
			enemyDirection = 0;
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

//		// Ici on essaye de rester close sinon random
		if(whoAmI != 1){
			for(String s : fetchAllMessages()){
				String tab[] = s.split("-");
				if(tab.length <= 1)
					continue;
				System.out.println("Jai recu "+tab[0]+"-"+tab[1]+"-"+tab[2]);
				Point leaderCoord = new Point(Integer.parseInt(tab[1]), Integer.parseInt(tab[2]));
				if(leaderCoord.distance(myCoords) >= DISTANCE_TO_LEADER){
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
	private void MyMove(){
		myCoords.setLocation(myCoords.getX() + Parameters.teamAMainBotSpeed * Math.cos(getHeading()), myCoords.getY() + Parameters.teamAMainBotSpeed * Math.sin(getHeading()));
		move();
	}
	private void MyMoveBack(){
		myCoords.setLocation(myCoords.getX() - Parameters.teamAMainBotSpeed * Math.cos(getHeading()), myCoords.getY() - Parameters.teamAMainBotSpeed * Math.sin(getHeading()));
		moveBack();
	}
	private void moveRandom(){
		/*** DEFAULT COMPORTEMENT ***/
		double randDouble = Math.random();
		isMoving = false;
		if(randDouble <= 0.60){
			isMoving = true;
			MyMove();
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
				MyMove();
				return;
			}else{
				MyMoveBack();
			}
		}
		else if(!doNotShoot){
			fire(enemyDirection);
			return;
		}
	}

	private boolean isHeading(double dir) {
		double heading = getHeading()%(2 * Math.PI);
		if(heading < 0)
			heading = heading + (2 * Math.PI);
		System.out.println("heading de "+ (heading -dir));
		if(heading- dir < 0)
			return Math.abs(heading - dir + (2 * Math.PI)) < ANGLEPRECISION;
		else
			return Math.abs(heading - dir)<ANGLEPRECISION;
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
		if(myCoords.x >= leaderCoord.x - DISTANCE_TO_LEADER/2  && myCoords.x <= leaderCoord.x + DISTANCE_TO_LEADER/2){
			if(myCoords.y >= leaderCoord.y - DISTANCE_TO_LEADER/2 && myCoords.y <= leaderCoord.y + DISTANCE_TO_LEADER/2){
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
		endRepositioningDirection = Parameters.NORTH + (2 * Math.PI);
	}
	private void descendre(){
		endRepositioningDirection = Parameters.SOUTH;
	}
	private void gauche(){
		endRepositioningDirection = Parameters.WEST;
	}
	private void droite(){
		endRepositioningDirection = Parameters.EAST;
	}
}
package asteroids;

import java.awt.event.*;
import java.util.Iterator;

import javax.swing.*;

import asteroids.participants.Asteroid;
import asteroids.participants.Ship;
import asteroids.participants.ShipBullet;
import static asteroids.Constants.*;

/**
 * Controls a game of Asteroids.
 */
public class Controller implements KeyListener, ActionListener
{
    // The state of all the Participants
    private ParticipantState pstate;
    
    // The ship (if one is active) or null (otherwise)
    private Ship ship;
    
    // When this timer goes off, it is time to refresh the animation
    private Timer refreshTimer;

    // The time at which a transition to a new stage of the game should be made.
    // A transition is scheduled a few seconds in the future to give the user
    // time to see what has happened before doing something like going to a new
    // level or resetting the current level.
    private long transitionTime;
    
    // Number of lives left
    private int lives;

    // The game display
    private Display display;
    
    // Number of Bullets from ship
    private int bulletCount = 0;

    /**
     * Constructs a controller to coordinate the game and screen
     */
    public Controller ()
    {
        // Record the game and screen objects
        display = new Display(this);
        display.setVisible(true);        
        
        // Initialize the ParticipantState
        pstate = new ParticipantState();

        // Set up the refresh timer.
        refreshTimer = new Timer(FRAME_INTERVAL, this);

        // Clear the transitionTime
        transitionTime = Long.MAX_VALUE;

        // Bring up the splash screen and start the refresh timer
        splashScreen();
        refreshTimer.start();
    }

    /**
     * Returns the ship, or null if there isn't one
     */
    public Ship getShip ()
    {
        return ship;
    }

    /**
     * Configures the game screen to display the splash screen
     */
    private void splashScreen ()
    {
        // Clear the screen, reset the level, and display the legend
        clear();
        display.setLegend("Asteroids");

        // Place four asteroids near the corners of the screen.
        placeAsteroids();
    }

    /**
     * The game is over. Displays a message to that effect.
     */
    private void finalScreen ()
    {
        display.setLegend(GAME_OVER);
        display.removeKeyListener(this);
    }

    /**
     * Place a new ship in the center of the screen. Remove any existing ship
     * first.
     */
    private void placeShip ()
    {
        // Place a new ship
        Participant.expire(ship);
        ship = new Ship(SIZE / 2, SIZE / 2, -Math.PI / 2, this);
        addParticipant(ship);
        display.setLegend("");
    }

    /**
     * Place a new bullet at the tip of ship heading in the direction of rotation of the ship.
     */
    public void placeBullet (){
        // Place a new bullet
        ShipBullet sB = new ShipBullet(150d, 150d, 5d, ship.getRotation(), 1d, this);
        sB.setPosition(ship.getXNose(), ship.getYNose());
        sB.setDirection(ship.getRotation());
        addParticipant(sB);
        
    }
    
    /**
     * Places four asteroids near the corners of the screen. Gives them random
     * velocities and rotations.
     */
    private void placeAsteroids ()
    {
        addParticipant(new Asteroid(0, 2, EDGE_OFFSET, EDGE_OFFSET, 3, this));
        addParticipant(new Asteroid(1, 2, SIZE - EDGE_OFFSET, EDGE_OFFSET, 3, this));
        addParticipant(new Asteroid(2, 2, EDGE_OFFSET, SIZE - EDGE_OFFSET, 3, this));
        addParticipant(new Asteroid(3, 2, SIZE - EDGE_OFFSET, SIZE - EDGE_OFFSET, 3, this));
    }

    /**
     * Clears the screen so that nothing is displayed
     */
    private void clear ()
    {
        pstate.clear();
        display.setLegend("");
        ship = null;
    }

    /**
     * Sets things up and begins a new game.
     */
    private void initialScreen ()
    {
        // Clear the screen
        clear();

        // Place four asteroids
        placeAsteroids();

        // Place the ship
        placeShip();
       

        // Reset statistics
        lives = 3;

        // Start listening to events
        display.addKeyListener(this);

        // Give focus to the game screen
        display.requestFocusInWindow();
    }

    /**
     * Adds a new Participant
     */
    public void addParticipant (Participant p)
    {
        pstate.addParticipant(p);
    }

    /**
     * The ship has been destroyed
     */
    public void shipDestroyed ()
    {
        // Null out the ship
        ship = null;
        
        // Display a legend
        display.setLegend("Ouch!");

        // Decrement lives
        lives--;

        // Since the ship was destroyed, schedule a transition
        scheduleTransition(END_DELAY);
    }

    /**
     * An asteroid of the given size has been destroyed
     */
    public void asteroidDestroyed (int size)
    {
        // If all the asteroids are gone, schedule a transition
        if (pstate.countAsteroids() == 0)
        {
            scheduleTransition(END_DELAY);
        }
    }

    /**
     * A bullet has been destroyed
     */
    public void bulletDestroyed ()
    {
        // Decrement bulletCount
        if (bulletCount > 0)
    		bulletCount--;
    }
    
    /**
     * Schedules a transition m msecs in the future
     */
    private void scheduleTransition (int m)
    {
        transitionTime = System.currentTimeMillis() + m;
    }

    /**
     * This method will be invoked because of button presses and timer events.
     */
    @Override
    public void actionPerformed (ActionEvent e)
    {
        // The start button has been pressed. Stop whatever we're doing
        // and bring up the initial screen
        if (e.getSource() instanceof JButton)
        {
            initialScreen();
        }

        // Time to refresh the screen and deal with keyboard input
        else if (e.getSource() == refreshTimer)
        {
            // It may be time to make a game transition
            performTransition();

            // Move the participants to their new locations
            pstate.moveParticipants();
//            if (ship != null){
//            	ship.turnLeft();
//            }

            // Refresh screen
            display.refresh();
        }
    }

    /**
     * Returns an iterator over the active participants
     */
    public Iterator<Participant> getParticipants ()
    {
        return pstate.getParticipants();
    }

    /**
     * If the transition time has been reached, transition to a new state
     */
    private void performTransition ()
    {
        // Do something only if the time has been reached
        if (transitionTime <= System.currentTimeMillis())
        {
            // Clear the transition time
            transitionTime = Long.MAX_VALUE;

            // If there are no lives left, the game is over. Show the final
            // screen.
            if (lives <= 0)
            {
                finalScreen();
            }

            // If the ship was destroyed, place a new one and continue
            else if (ship == null)
            {
                placeShip();
            }
        }
    }

    /**
     * If a key of interest is pressed, record that it is down.
     */
    @Override
    public void keyPressed (KeyEvent e)
    {
        if (e.getKeyCode() == KeyEvent.VK_LEFT && ship != null)
        {
            ship.turnLeft();
        }
        else if (e.getKeyCode() == KeyEvent.VK_RIGHT && ship != null)
        {
            ship.turnRight();
        }
        else if (e.getKeyCode() == KeyEvent.VK_DOWN && ship != null)
        {
        	if (bulletCount < 8){
        		ship.shootBullet();
        		bulletCount++;
        	}
        }
    }

    /**
     * Ignore these events.
     */
    @Override
    public void keyTyped (KeyEvent e)
    {
    }

    /**
     * Ignore these events.
     */
    @Override
    public void keyReleased (KeyEvent e)
    {      
    }
    
    public void bulletBack(int b){
    	bulletCount += b;
    }
}

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Arc2D;
import java.io.File;
import java.io.IOException;

import com.engine.core.*;
import com.engine.core.gfx.*;

public class Main extends AbstractGame
{
    //Required Basic Game Functional Data
    private static GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    private static int screenWidth = device.getDisplayMode().getWidth();
    private static int screenHeight = device.getDisplayMode().getHeight();

    //Required Basic Game Visual data used in main below
    private static String gameName = "My Game";
    private static int windowWidth = 960;	//For fullscreen mode set these next two to screenWidth and screenHeight
    private static int windowHeight = 600;
    private static int fps = 60;

    SpriteSheet backgroundImg;
    SpriteSheet playerPaddleImg;
    SpriteSheet enemyPaddleImg;
    SpriteSheet ballImg;
    SpriteSheet[] playerBrickSprites = new SpriteSheet[7];
    SpriteSheet[] enemyBrickSprites = new SpriteSheet[7];

    Rectangle[] playerBrickRects = new Rectangle[playerBrickSprites.length];
    Rectangle[] enemyBrickRects = new Rectangle[enemyBrickSprites.length];
    Rectangle[] boundaryRects = new Rectangle[2];
    Rectangle playerPaddleRect = new Rectangle();
    Rectangle enemyPaddleRect = new Rectangle();
    Rectangle ballRect = new Rectangle();

    Font msgFont;

    Vector2F ballSpeed = new Vector2F(-3, 0);
    int playerScore = 0;
    int enemyScore = 0;

    int mode = 0;
    long startTime = System.currentTimeMillis();
    float playerPaddleVelocity = 0;
    float enemyPaddleVelocity = 0;

    boolean movedToPos = false; // Rename this!
    int pos; // Rename this!

    public static void main(String[] args)
    {
        GameContainer gameContainer = new GameContainer(new Main(), gameName, windowWidth, windowHeight, fps);
        gameContainer.Start();
    }

    //super speed, large paddle, ice friction, smalelr paddle

    @Override
    public void LoadContent(GameContainer gc)
    {
        //TODO: This subprogram automatically happens only once, just before the actual game loop starts.
        //It should never be manually called, the Engine will call it for you.
        //Load images, sounds and set up any data

        backgroundImg = new SpriteSheet(LoadImage.FromFile("/images/backgrounds/background.png"));
        backgroundImg.destRec = new Rectangle(0, 0, windowWidth, windowHeight);

        playerPaddleImg = new SpriteSheet(LoadImage.FromFile("/images/sprites/paddle.png"));
        playerPaddleImg.destRec = new Rectangle(100, windowHeight/3 + windowHeight/10, playerPaddleImg.GetFrameWidth() + (playerPaddleImg.GetFrameWidth() / 2), playerPaddleImg.GetFrameHeight() + playerPaddleImg.GetFrameHeight() / 2);

        enemyPaddleImg = new SpriteSheet(LoadImage.FromFile("/images/sprites/paddle.png"));
        enemyPaddleImg.destRec = new Rectangle(825, windowHeight/3 + windowHeight/10, enemyPaddleImg.GetFrameWidth() + (enemyPaddleImg.GetFrameWidth()/2), enemyPaddleImg.GetFrameHeight() + (enemyPaddleImg.GetFrameHeight() / 2));

        ballImg = new SpriteSheet(LoadImage.FromFile("/images/sprites/ball.png"));
        ballImg.destRec = new Rectangle(windowWidth/2, windowHeight/2, ballImg.GetFrameWidth() + (ballImg.GetFrameWidth() / 2), ballImg.GetFrameHeight() + (ballImg.GetFrameHeight() / 2));

        boundaryRects[0] = new Rectangle(0, 0, windowWidth, 1);
        boundaryRects[1] = new Rectangle(0, windowHeight, windowWidth, 1);

        for (int i = 0, j = 0; i < playerBrickSprites.length; i++, j++)
        {
            playerBrickSprites[i] = new SpriteSheet(LoadImage.FromFile("/images/sprites/brick.png"));
            playerBrickSprites[i].destRec = new Rectangle(20, j * 75 + 40, playerBrickSprites[i].GetFrameWidth(), playerBrickSprites[i].GetFrameHeight());
        }

        for (int i = 0, j = 0; i < enemyBrickSprites.length; i++, j++)
        {
            enemyBrickSprites[i] = new SpriteSheet(LoadImage.FromFile("/images/sprites/brick.png"));
            enemyBrickSprites[i].destRec = new Rectangle((windowWidth - 40), j * 75 + 40, enemyBrickSprites[i].GetFrameWidth(), enemyBrickSprites[i].GetFrameHeight());
        }

        try
        {
            msgFont = Font.createFont(Font.TRUETYPE_FONT, new File("./res/fonts/bit5x3.ttf")).deriveFont(Font.PLAIN, 80);
        }
        catch (FontFormatException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void UpdateImageRectangles()
    {
        for (int i = 0; i < playerBrickRects.length; i++)
        {
            playerBrickRects[i] = new Rectangle(playerBrickSprites[i].destRec.x, playerBrickSprites[i].destRec.y, playerBrickSprites[i].GetFrameWidth(), playerBrickSprites[i].GetFrameHeight());
        }

        for (int i = 0; i < enemyBrickRects.length; i++)
        {
            enemyBrickRects[i] = new Rectangle(enemyBrickSprites[i].destRec.x, enemyBrickSprites[i].destRec.y, enemyBrickSprites[i].GetFrameWidth(), enemyBrickSprites[i].GetFrameHeight());
        }

        playerPaddleRect = new Rectangle(playerPaddleImg.destRec.x, playerPaddleImg.destRec.y, playerPaddleImg.GetFrameWidth() + (playerPaddleImg.GetFrameWidth() / 2), playerPaddleImg.GetFrameHeight() + (playerPaddleImg.GetFrameHeight() / 2));
        enemyPaddleRect = new Rectangle(enemyPaddleImg.destRec.x, enemyPaddleImg.destRec.y, enemyPaddleImg.GetFrameWidth() + (enemyPaddleImg.GetFrameWidth() / 2), enemyPaddleImg.GetFrameHeight() + (enemyPaddleImg.GetFrameHeight() / 2));
        ballRect = new Rectangle(ballImg.destRec.x, ballImg.destRec.y, ballImg.GetFrameWidth() + (ballImg.GetFrameWidth() / 2), ballImg.GetFrameHeight() + (ballImg.GetFrameHeight() / 2));
    }

    private void CheckCollisions()
    {
        if (Helper.Intersects(playerPaddleRect, ballRect))
        {
            float oldSpeed = ballSpeed.x;
            ballSpeed.x = 0;
            ballSpeed.y = 0;

            double relativeIntersectY = (playerPaddleRect.y + (playerPaddleRect.height / 2)) - (ballRect.y + ballRect.height);
            double normalizedRelativeIntersectionY = (relativeIntersectY/(playerPaddleRect.height/2));
            double bounceAngle = normalizedRelativeIntersectionY * 5 * Math.PI / 12;

            if (ballRect.x >= playerPaddleRect.x)
            {
                int edgeLocation = playerPaddleRect.x + ballRect.width;

                ballImg.destRec.x = edgeLocation;

                if (Math.abs(oldSpeed) <= 15)
                {
                    ballSpeed.x = (oldSpeed * -1) + 1;
                }
                else
                {
                    ballSpeed.x = (oldSpeed * -1);
                }

                ballSpeed.y = 4 * (float)-Math.sin(bounceAngle);
            }
            else
            {
                ballSpeed.x = -oldSpeed;
                ballSpeed.y = 4 * (float)-Math.sin(bounceAngle);
            }
        }


        if (Helper.Intersects(enemyPaddleRect, ballRect))
        {
            float oldSpeed = ballSpeed.x;
            ballSpeed.x = 0;
            ballSpeed.y = 0;

            double relativeIntersectY = (enemyPaddleRect.y + (enemyPaddleRect.height / 2)) - (ballRect.y + ballRect.height);
            double normalizedRelativeIntersectionY = (relativeIntersectY/(enemyPaddleRect.height/2));
            double bounceAngle = normalizedRelativeIntersectionY * 5 * Math.PI / 12;

            if (ballRect.x <= enemyPaddleRect.x)
            {
                int farRightPos = ballRect.x + ballRect.width;
                int edgeLocation = ballRect.x - (farRightPos - enemyPaddleRect.x);

                ballImg.destRec.x = edgeLocation;

                if (Math.abs(oldSpeed) <= 15)
                {
                    ballSpeed.x = (oldSpeed * -1) - 1;
                }
                else
                {
                    ballSpeed.x = (oldSpeed * -1);
                }

                ballSpeed.y = 4 * (float)-Math.sin(bounceAngle);
            }
            else
            {
                ballImg.destRec.x = (enemyPaddleRect.x + enemyPaddleRect.width);
                ballSpeed.x = -oldSpeed;
                ballSpeed.y = 4 * (float)-Math.sin(bounceAngle);
            }
        }

        if (Helper.Intersects(ballRect, boundaryRects[0]))
        {
            if (Math.abs(ballSpeed.y) < 2) ballSpeed.y = ((int)Math.ceil(ballSpeed.y) * -1) + 1;
            else ballSpeed.y = ((int)Math.ceil(ballSpeed.y) * -1);
        }

        if (Helper.Intersects(ballRect, boundaryRects[1]))
        {
            if (Math.abs(ballSpeed.y) < 2) ballSpeed.y = ((int)Math.ceil(ballSpeed.y) * -1) - 1;
            else ballSpeed.y = ((int)Math.ceil(ballSpeed.y) * -1);
        }

        for (int i = 0; i < playerBrickRects.length; i++)
        {
            if (Helper.Intersects(ballRect, playerBrickRects[i]))
            {
                ballSpeed.x *= -1;
                ballSpeed.y *= -1;
                playerBrickSprites[i].destRec = new Rectangle(0, 0, 0, 0);

                playerScore++;
            }
        }
        for (int i = 0; i < enemyBrickRects.length; i++)
        {
            if (Helper.Intersects(ballRect, enemyBrickRects[i]))
            {
                enemyBrickSprites[i].destRec = new Rectangle(0, 0, 0, 0);
                ballSpeed.x *= -1;
                ballSpeed.y *= -1;

                enemyScore++;
            }
        }
    }

    private void CheckPaddleBounds()
    {
        if (playerPaddleRect.y < 0) playerPaddleImg.destRec.y = 0;
        if (playerPaddleRect.y + playerPaddleRect.height > windowHeight) playerPaddleImg.destRec.y = windowHeight - playerPaddleRect.height;

        if (enemyPaddleRect.y < 0) enemyPaddleImg.destRec.y = 0;
        if (enemyPaddleRect.y + enemyPaddleRect.height > windowHeight) enemyPaddleImg.destRec.y = windowHeight - enemyPaddleRect.height;
    }

    private void ResetBallPosition(boolean playerGoal)
    {
        ballImg.destRec = new Rectangle(windowWidth/2, windowHeight/2, ballImg.GetFrameWidth() + (ballImg.GetFrameWidth() / 2), ballImg.GetFrameHeight() + (ballImg.GetFrameHeight() / 2));
        if (playerGoal) ballSpeed.x = 2;
        else ballSpeed.x = -2;
        ballSpeed.y = 0;

        MoveToPosition(enemyPaddleRect, windowHeight/2 - enemyPaddleRect.height);
    }

    private void BallOutOfBounds()
    {
        if (ballRect.x > windowWidth)
        {
            ResetBallPosition(false);
            playerScore += 2;
        }
        else if (ballRect.x < 0)
        {
            ResetBallPosition(true);
            enemyScore += 2;
        }
    }

    private void MoveToPosition(Rectangle paddle, int y)
    {
        if (!movedToPos)
        {
            int paddleCenter = paddle.y + paddle.height/2;

            if (Math.abs(paddleCenter - y) <= 5)
            {
                return;
            }

            if (paddleCenter > y)
            {
                enemyPaddleVelocity = -3;
            }
            else if (paddleCenter < y)
            {
                enemyPaddleVelocity = 3;
            }

            movedToPos = true;
        }
        else
        {
            enemyPaddleVelocity *= 0.94;

            if ((enemyPaddleVelocity < 0.05 && enemyPaddleVelocity > 0) || (enemyPaddleVelocity > -0.4 && enemyPaddleVelocity < 0))
            {
                enemyPaddleVelocity = 0;
            }
        }

        enemyPaddleImg.destRec.y += enemyPaddleVelocity;
    }


    private void AI()
    {
        int paddleCentre = enemyPaddleRect.y + enemyPaddleRect.height/2;
        float ballCentre = ballRect.y + ballRect.height/2;

        pos = (int)ballCentre;
        MoveToPosition(enemyPaddleRect, pos);

        if (ballSpeed.x > 0)
        {
            movedToPos = false;
        }

        /*
        if (ballSpeed.x < 0)
        {
            enemyPaddleVelocity *= 0.94;

            if ((enemyPaddleVelocity < 0.05 && enemyPaddleVelocity > 0) || (enemyPaddleVelocity > -0.4 && enemyPaddleVelocity < 0))
            {
                enemyPaddleVelocity = 0;
            }
            return;
        }
        else if (ballCentre > paddleCentre)
        {
            enemyPaddleVelocity = 3;
        }
        else if (ballCentre < paddleCentre)
        {
            enemyPaddleVelocity = -3;
        }
        else if ()

        enemyPaddleImg.destRec.y += enemyPaddleVelocity;
        */
    }

    @Override
    public void Update(GameContainer gc, float deltaTime)
    {
        //TODO: Add your update logic here, including user input, movement, physics, collision, ai, sound, etc.

        if (mode == 3 || mode == 4)
        {
            if (Input.IsKeyDown(KeyEvent.VK_S))
            {
                 playerPaddleVelocity = 5;
            }
            else if (Input.IsKeyDown(KeyEvent.VK_W))
            {
               playerPaddleVelocity = -5;
            }
            else
            {
                playerPaddleVelocity *= 0.94;

                if ((playerPaddleVelocity < 0.05 && playerPaddleVelocity > 0) || (playerPaddleVelocity > -0.4 && playerPaddleVelocity < 0))
                {
                    playerPaddleVelocity = 0;
                }
            }

            playerPaddleImg.destRec.y += playerPaddleVelocity;
            /*
            if (Input.IsKeyDown(KeyEvent.VK_DOWN)) enemyPaddleImg.destRec.y += 5;
            else if (Input.IsKeyDown(KeyEvent.VK_UP)) enemyPaddleImg.destRec.y -= 5;
            */

            AI();

            ballImg.destRec.x += ballSpeed.x;
            ballImg.destRec.y += ballSpeed.y;

            UpdateImageRectangles();
            CheckCollisions();
            BallOutOfBounds();
            CheckPaddleBounds();
        }

        if (Input.IsKeyPressed(KeyEvent.VK_ENTER) && mode != 1)
        {
            mode++;
        }

        GetGameType();
    }

    @Override
    public void Draw(GameContainer gc, Graphics2D gfx)
    {
        //TODO: Add your draw logic here

        if (mode == 0) OpeningSequence(gfx);
        if (mode == 1) ModeSelectScreen(gfx);
        if (mode == 2) SingleplayerPlayScreen(gfx);
        if (mode == 3) MultiplayerPlayScreen(gfx);
    }

    private void OpeningSequence(Graphics2D gfx)
    {

    }

    private void ModeSelectScreen(Graphics2D gfx)
    {
        Draw.Text(gfx, "1. singleplayer", 150, 100, msgFont, Helper.GetColor(255,255,255), 1);
        Draw.Text(gfx, "2. multiplayer", 150, 300, msgFont, Helper.GetColor(255,255,255), 1);
    }

    private void SingleplayerPlayScreen(Graphics2D gfx)
    {

    }

    private void MultiplayerPlayScreen(Graphics2D gfx)
    {
        Draw.Sprite(gfx, backgroundImg);

        // Text
        Draw.Text(gfx, String.valueOf(playerScore), 150, 100, msgFont, Helper.GetColor(255,255,255), 1);
        Draw.Text(gfx, String.valueOf(enemyScore), 760, 100, msgFont, Helper.GetColor(255,255,255), 1);

        // Images
        Draw.Sprite(gfx, playerPaddleImg);
        Draw.Sprite(gfx, enemyPaddleImg);
        Draw.Sprite(gfx, ballImg);

        //Draw.FillRect(gfx, ballRect.x, ballRect.y, ballRect.width, ballRect.height, Helper.RED, 0.5f);
        for (int i = 0; i < playerBrickSprites.length; i++)
        {
            Draw.Sprite(gfx, playerBrickSprites[i]);
        }

        for (int i = 0; i < enemyBrickSprites.length; i++)
        {
            Draw.Sprite(gfx, enemyBrickSprites[i]);
        }
    }

    private void GetGameType()
    {
        if (Input.IsKeyReleased(KeyEvent.VK_1) && mode == 1)
        {
            mode++;
        }
        else if (Input.IsKeyPressed(KeyEvent.VK_2) && mode == 1)
        {
            mode = 3;
        }
    }
}

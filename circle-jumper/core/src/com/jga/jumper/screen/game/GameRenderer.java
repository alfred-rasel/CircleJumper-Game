package com.jga.jumper.screen.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.jga.jumper.assets.AssetDescriptors;
import com.jga.jumper.assets.RegionNames;
import com.jga.jumper.common.GameManager;
import com.jga.jumper.common.GameState;
import com.jga.jumper.config.GameConfig;
import com.jga.jumper.entity.Coin;
import com.jga.jumper.entity.Monster;
import com.jga.jumper.entity.Obstacle;
import com.jga.jumper.entity.Planet;
import com.jga.jumper.screen.menu.GameOverOverlay;
import com.jga.jumper.screen.menu.MenuOverlay;
import com.jga.util.ViewportUtils;
import com.jga.util.debug.DebugCameraController;

public class GameRenderer implements Disposable {

    private final GameController controller;

    private final SpriteBatch batch;
    private final AssetManager assetManager;


    private OrthographicCamera camera;
    private Viewport viewport;
    private ShapeRenderer renderer;
    private Viewport hudViewport;
    private BitmapFont font;


    private final GlyphLayout layout = new GlyphLayout();


    private DebugCameraController debugCameraController;

    private TextureRegion backgroundRegion;
    private TextureRegion planetRegion;

    private Animation obstacleAnimation;
    private Animation coinAnimation;
    private Animation monsterAnimation;

    private Stage hudStage;
    private MenuOverlay menuOverlay;
    private GameOverOverlay gameOverOverlay;


    public GameRenderer(GameController controller, SpriteBatch batch, AssetManager assetManager) {
        this.controller = controller;
        this.batch = batch;
        this.assetManager = assetManager;
        init();
    }

    private void init() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT, camera);
        renderer = new ShapeRenderer();


        hudViewport = new FitViewport(GameConfig.HUD_WIDTH, GameConfig.HUD_HEIGHT);
        hudStage = new Stage(hudViewport, batch);


        font = assetManager.get(AssetDescriptors.FONT);

        Skin skin = assetManager.get(AssetDescriptors.SKIN);


        debugCameraController = new DebugCameraController();
        debugCameraController.setStartPosition(GameConfig.WORLD_CENTER_X, GameConfig.WORLD_CENTER_Y);

        TextureAtlas gameplayAtlas = assetManager.get(AssetDescriptors.GAME_PLAY);

        backgroundRegion = gameplayAtlas.findRegion(RegionNames.BACKGROUND);
        planetRegion = gameplayAtlas.findRegion(RegionNames.PLANET);

        obstacleAnimation = new Animation(0.1f,
                gameplayAtlas.findRegions(RegionNames.OBSTACLE),
                Animation.PlayMode.LOOP_PINGPONG);
        coinAnimation = new Animation(0.2f,
                gameplayAtlas.findRegions(RegionNames.COIN),
                Animation.PlayMode.LOOP_PINGPONG);
        monsterAnimation = new Animation(0.05f,

                gameplayAtlas.findRegions(RegionNames.PLAYER),
                Animation.PlayMode.LOOP_PINGPONG);

        menuOverlay = new MenuOverlay(skin, controller.getCallback());
        gameOverOverlay = new GameOverOverlay(skin, controller.getCallback());

        hudStage.addActor(menuOverlay);
//        hudStage.setDebugAll(true);
        hudStage.addActor(gameOverOverlay);
        //   hudStage.setDebugAll(true);

        Gdx.input.setInputProcessor(hudStage);
    }

    public void render(float delta) {
        debugCameraController.handleDebugInput(delta);
        debugCameraController.applyTo(camera);

        renderGameplay(delta);
        renderDebug();

        renderHud();

    }

    public void resize(int width, int height) {
        viewport.update(width, height, true);
        hudViewport.update(width, height, true);
        ViewportUtils.debugPixelsPerUnit(viewport);

    }

    @Override
    public void dispose() {
        renderer.dispose();
    }

    private void renderGameplay(float delta) {
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        drawGamePlay(delta);

        batch.end();
    }

    private void drawGamePlay(float delta) {

        //background
        batch.draw(backgroundRegion,
                0, 0,
                GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT);

        //obstacles
        Array<Obstacle> obstacles = controller.getObstacles();
        TextureRegion obstacleRegion = (TextureRegion) obstacleAnimation.getKeyFrame(controller.getAnimationTime());

        for (Obstacle obstacle : obstacles) {
            batch.draw(obstacleRegion,
                    obstacle.getX(), obstacle.getY(),
                    0, 0,
                    obstacle.getWidth(), obstacle.getHeight(),
                    1.0f, 1.0f,
                    GameConfig.START_ANGLE - obstacle.getAngleDeg()
            );
        }

        //planet
        Planet planet = controller.getPlanet();
        batch.draw(planetRegion,
                planet.getX(), planet.getY(),
                planet.getWidth(), planet.getHeight());


        //coins
        Array<Coin> coins = controller.getCoins();
        TextureRegion coinRegion = (TextureRegion) coinAnimation.getKeyFrame(controller.getAnimationTime());
        for (Coin coin : coins) {
            batch.draw(coinRegion,
                    coin.getX(), coin.getY(),
                    0, 0,
                    coin.getWidth(), coin.getHeight(),
                    coin.getScale(), coin.getScale(),
                    GameConfig.START_ANGLE - coin.getAngleDeg()
            );
        }

        //monster
        Monster monster = controller.getMonster();
        TextureRegion monsterRegion = (TextureRegion) monsterAnimation.getKeyFrame(controller.getAnimationTime());

        batch.draw(monsterRegion,
                monster.getX(), monster.getY(),
                0, 0,
                monster.getWidth(), monster.getHeight(),
                1.0f, 1.0f,
                GameConfig.START_ANGLE - monster.getAngleDeg()
        );

    }

    private void renderDebug() {
        ViewportUtils.drawGrid(viewport, renderer, GameConfig.CELL_SIZE);
        viewport.apply();
        renderer.setProjectionMatrix(camera.combined);
        renderer.begin(ShapeRenderer.ShapeType.Line);

        drawDebug();

        renderer.end();


    }

    private void drawDebug() {
        //planet
        renderer.setColor(Color.RED);
        Planet planet = controller.getPlanet();
        Circle planetBounds = planet.getBounds();
        renderer.circle(planetBounds.x, planetBounds.y, planetBounds.radius, 30);

        //monster
        renderer.setColor(Color.BLUE);
        Monster monster = controller.getMonster();
        Rectangle monsterBounds = monster.getBounds();
        renderer.rect(
                monsterBounds.x, monsterBounds.y,
                0, 0,
                monsterBounds.width, monsterBounds.height,
                1, 1,
                GameConfig.START_ANGLE - monster.getAngleDeg()
        );

        //coin
        renderer.setColor(Color.YELLOW);
        for (Coin coin : controller.getCoins()) {
            Rectangle coinBounds = coin.getBounds();
            renderer.rect(
                    coinBounds.x, coinBounds.y,
                    0, 0,
                    coinBounds.width, coinBounds.height,
                    coin.getScale(), coin.getScale(),
                    GameConfig.START_ANGLE - coin.getAngleDeg()
            );

        }

        //obstacles
        for (Obstacle obstacle : controller.getObstacles()) {
            //obstacle
            renderer.setColor(Color.GREEN);
            Rectangle obstacleBounds = obstacle.getBounds();
            renderer.rect(
                    obstacleBounds.x, obstacleBounds.y,
                    0, 0,
                    obstacleBounds.width, obstacleBounds.height,
                    1, 1,
                    GameConfig.START_ANGLE - obstacle.getAngleDeg()
            );

            //sensor
            renderer.setColor(Color.WHITE);
            Rectangle sensorBounds = obstacle.getSensor();
            renderer.rect(
                    sensorBounds.x, sensorBounds.y,
                    0, 0,
                    sensorBounds.width, sensorBounds.height,
                    1, 1,
                    GameConfig.START_ANGLE - obstacle.getSensorAngleDeg()
            );
        }
    }

    private void renderHud() {
        hudViewport.apply();

        menuOverlay.setVisible(false);
        gameOverOverlay.setVisible(false);

        GameState gameState = controller.getGameState();

        if (gameState.isPlayingOrReady()) {
            batch.setProjectionMatrix(hudViewport.getCamera().combined);
            batch.begin();

            drawHud();

            batch.end();
            return;
        }
        if (gameState.isMenu() && !menuOverlay.isVisible()) {
            menuOverlay.updateLabel();
            menuOverlay.setVisible(true);
        } else if (gameState.isGameOver() && !gameOverOverlay.isVisible()) {
            gameOverOverlay.updateLabels();
            gameOverOverlay.setVisible(true);
        }

        hudStage.act();
        hudStage.draw();
    }

    private void drawHud() {
        // high score
        float padding = 20;
        String highScoreString = "HIGH SCORE:" + GameManager.INSTANCE.getDisplayHighScore();
        layout.setText(font, highScoreString);
        font.draw(batch, layout, padding, GameConfig.HUD_HEIGHT - layout.height);

        // score
        String scoreString = "SCORE:" + GameManager.INSTANCE.getDisplayScore();
        layout.setText(font, scoreString);
        font.draw(batch, layout,
                GameConfig.HUD_WIDTH - layout.width - padding,
                GameConfig.HUD_HEIGHT - layout.height
        );

        float startWaitTimer = controller.getStartWaitTimer();

        if (startWaitTimer >= 0) {
            int waitTime = (int) startWaitTimer;
            String waitTimerString = waitTime == 0 ? "GO" : "" + waitTime;
            layout.setText(font, waitTimerString);

            font.draw(
                    batch, layout,
                    (GameConfig.HUD_WIDTH - layout.width) / 2f,
                    (GameConfig.HUD_HEIGHT + layout.height) / 2f
            );
        }
        Color oldFontColor = new Color(font.getColor());

        for(FloatingScore floatingScore : controller.getFloatingScore()){
            layout.setText(font floatingScore.getScoreString());
            font.draw(batch,layout,
               floatingScore.getX() = layout.width/2f;
                floatingScore.getY() = layout.height/2f);
        }
        font.setColor(oldFontColor);
    }
}

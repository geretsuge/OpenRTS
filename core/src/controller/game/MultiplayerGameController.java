package controller.game;

import geometry.geom2d.AlignedBoundingBox;
import geometry.geom2d.Point2D;

import java.util.ArrayList;
import java.util.List;

import model.ModelManager;
import model.battlefield.army.ArmyManager;
import model.battlefield.army.components.Unit;
import view.MapView;
import view.math.Translator;

import com.google.common.eventbus.Subscribe;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.InputManager;
import com.jme3.renderer.Camera;

import controller.CommandManager;
import controller.Controller;
import controller.cameraManagement.IsometricCameraManager;
import de.lessvoid.nifty.Nifty;
import event.BattleFieldUpdateEvent;
import event.ControllerChangeEvent;
import event.EventManager;

public class MultiplayerGameController extends Controller {

	private boolean paused = false;
	private Point2D zoneStart;
	private boolean drawingZone = false;
	protected MapView view;

	public MultiplayerGameController(MapView view, Nifty nifty, InputManager inputManager, Camera cam) {
		super(view, inputManager, cam);
		this.view = view;
		inputInterpreter = new MultiplayerGameInputInterpreter(this);
		this.spatialSelector.centered = false;
		guiController = new MultiplayerGameNiftyController(nifty, this);

		EventManager.registerForClient(this);

		cameraManager = new IsometricCameraManager(cam, 10);
	}

	@Override
	public void update(float elapsedTime) {
		updateSelectionZone();
		updateContext();
		guiController.update();

		// update army
		if (!paused) {
			ArmyManager.update(elapsedTime);
		}
	}

	public void startSelectionZone() {
		zoneStart = getMouseCoord();
	}

	public void endSelectionZone() {
		zoneStart = null;
		drawingZone = false;
	}

	public boolean isDrawingZone() {
		return drawingZone;
	}

	private void updateSelectionZone() {
		if (zoneStart == null) {
			view.getGuiNode().detachAllChildren();
			return;
		}

		Point2D coord = getMouseCoord();
		if (coord.equals(zoneStart)) {
			return;
		}

		drawingZone = coord.getDistance(zoneStart) > 10;

		AlignedBoundingBox rect = new AlignedBoundingBox(zoneStart, coord);
		List<Unit> inSelection = new ArrayList<>();
		for (Unit u : ArmyManager.getUnits()) {
			if (rect.contains(spatialSelector.getScreenCoord(u.getPos()))) {
				inSelection.add(u);
			}
		}
		CommandManager.select(inSelection);
		view.drawSelectionArea(zoneStart, coord);
	}

	private void updateContext() {
		AlignedBoundingBox screen = new AlignedBoundingBox(Point2D.ORIGIN, cameraManager.getCamCorner());
		List<Unit> inScreen = new ArrayList<>();
		for (Unit u : ArmyManager.getUnits()) {
			if (screen.contains(spatialSelector.getScreenCoord(u.getPos()))) {
				inScreen.add(u);
			}
		}
		CommandManager.createContextualUnities(inScreen);

	}

	@Subscribe
	public void manageEvent(ControllerChangeEvent ev) {
		guiController.update();
	}

	@Subscribe
	public void manageEvent(BattleFieldUpdateEvent ev) {
		((IsometricCameraManager) cameraManager).move(ModelManager.getBattlefield().getMap().width / 2, ModelManager.getBattlefield().getMap().height / 2);
	}

	// TODO: See AppState.setEnabled => use it, this is a better implementation
	public void togglePause() {
		paused = !paused;
		view.getActorManager().pause(paused);
	}

	@Override
	public void stateAttached(AppStateManager stateManager) {
		super.stateAttached(stateManager);
		inputManager.setCursorVisible(true);
		guiController.activate();
	}

	private Point2D getMouseCoord() {
		return Translator.toPoint2D(inputManager.getCursorPosition());
	}

}
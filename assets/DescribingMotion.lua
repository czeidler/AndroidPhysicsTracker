function onBuildExperimentScript(scriptBuilder)
	local intro = scriptBuilder:create("Sheet")
	scriptBuilder:add(intro)
	intro:setTitle("Stage 1 Physics Laboratory")
	intro:addHeader("Lab equipment:")
	intro:addText("Have you got the lab equipment? Check the following:")
	intro:addCheckQuestion("a metre rule for setting the length scale in the video")
	intro:addCheckQuestion("a ball")

	local takeVideosSheet = scriptBuilder:create("Sheet")
	scriptBuilder:add(takeVideosSheet)
	takeVideosSheet:setMainLayoutOrientation("horizontal")
	takeVideosSheet:setTitle("Take Videos:")
	cameraExperimentItem = takeVideosSheet:addCameraExperiment();
	cameraExperimentItem:setDescriptionText("Please take a free fall video:")
	local experimentFreeFall = cameraExperimentItem:getExperiment()
	cameraExperimentItem2 = takeVideosSheet:addCameraExperiment();
	cameraExperimentItem2:setDescriptionText("Please take a up down video:")
	local experimentUpDown = cameraExperimentItem2:getExperiment()
	cameraExperimentItem3 = takeVideosSheet:addCameraExperiment()
	cameraExperimentItem3:setDescriptionText("Please take a projectile video:")
	local experimentProjectile = cameraExperimentItem3:getExperiment()

	local info = scriptBuilder:create("Sheet")
	scriptBuilder:add(info)
	info:setTitle("Info")
	info:addText("Check with your demonstrator about your videos and get ticked off before your proceed to video analysis.")
	
	info:addText("Video analysis: In this section, you and your lab partner will track the motion of the ball. For all three videos follow these steps:")
	info:addText("Go to \"Video Settings\". Set the start and end frames. For example, the free-fall video should start shortly before the ball leaves your hand and shortly after the ball hits the ground. Optionally, increase the frame rate to 15 or 30 frames per second. Click \"Apply\".")
	info:addText("Now drag the green length scale to fit your length reference. Click on \"Calibration\" and set scale. Click \"Apply\".")
	info:addText("You can now tag the positions of the ball from the initial position to the final position. Find the cross-hair with green rings, drag it by the outer ring to tag the ball and advance to the next frame. Repeat until you have finished tagging. Click “Done” to analyse other videos (e.g. vertical linear motion and projectile motion).")

	-- tagging
	local experimentAnalysisFreeFall = scriptBuilder:create("ExperimentAnalysis")
	scriptBuilder:add(experimentAnalysisFreeFall)
	experimentAnalysisFreeFall:setTitle("Mark Data Points")
	experimentAnalysisFreeFall:setExperiment(experimentFreeFall)
	experimentAnalysisFreeFall:setDescriptionText("Please analyse the free fall video:")

	local experimentAnalysisUpDown = scriptBuilder:create("ExperimentAnalysis")
	scriptBuilder:add(experimentAnalysisUpDown)
	experimentAnalysisUpDown:setTitle("Mark Data Points")
	experimentAnalysisUpDown:setExperiment(experimentUpDown)
	experimentAnalysisUpDown:setDescriptionText("Please analyse the up down video:")

	local experimentAnalysisProjectile = scriptBuilder:create("ExperimentAnalysis")
	scriptBuilder:add(experimentAnalysisProjectile)
	experimentAnalysisProjectile:setTitle("Mark Data Points")
	experimentAnalysisProjectile:setExperiment(experimentProjectile)
	experimentAnalysisProjectile:setDescriptionText("Please analyse the projectile video:")

	-- analysis

	-- free fall motion
	local calculateYSpeed = scriptBuilder:create("CalculateYSpeed")
	scriptBuilder:add(calculateYSpeed)
	calculateYSpeed:setExperiment(experimentFreeFall)
	calculateYSpeed:setTitle("Free fall")
	calculateYSpeed:setHeader("Deriving average y-velocity and y-acceleration from displacement")

	local freeFallQuestions = scriptBuilder:create("Sheet")
	scriptBuilder:add(freeFallQuestions)
	freeFallQuestions:setTitle("Free Fall")
	freeFallQuestions:addHeader("Estimating impact velocity")
	freeFallQuestions:addText("Go to your demonstrator once you finished answering the questions on this page:")
	local graphLayout = freeFallQuestions:addHorizontalGroupLayout()
	freeFallQuestions:addPositionGraph(experimentFreeFall, graphLayout)
	freeFallQuestions:addYSpeedGraph(experimentFreeFall, graphLayout)
	freeFallQuestions:addQuestion("We know that moving objects have kinetic energy and that energy can be transferred from one form to the other. How did the ball gain kinetic energy?")
	freeFallQuestions:addQuestion("Use the “Conservation of Energy” principle to estimate the impact velocity of the ball. How does it compare with your observed final velocity? If there is any difference, suggest the possible causes.")
	freeFallQuestions:addCheckQuestion("Go to your demonstrator now and verify your results.")

	-- up down motion
	local upDownMotionQuestions = scriptBuilder:create("Sheet")
	scriptBuilder:add(upDownMotionQuestions)
	upDownMotionQuestions:setTitle("Vertical linear motion")
	upDownMotionQuestions:addHeader("Analysing graphs:")
	upDownMotionQuestions:addText("Use the position-time graph and the velocity-time graph to complete the questions below.")
	local graphLayout = upDownMotionQuestions:addHorizontalGroupLayout()
	local graph = upDownMotionQuestions:addGraph(experimentUpDown, graphLayout)
	graph:setTitle("y-Position vs. Time")
	graph:setXAxisContent("time")
	graph:setYAxisContent("y-position")
	local graph = upDownMotionQuestions:addGraph(experimentUpDown, graphLayout)
	graph:setTitle("y-Velocity vs. Time")
	graph:setXAxisContent("time_v")
	graph:setYAxisContent("y-velocity")
	upDownMotionQuestions:addQuestion("How does the vertical velocity vary with time?")
	upDownMotionQuestions:addQuestion("Point out on the height-time graph where the vertical velocity of the ball was the maximum and also where it was zero.")
	
	upDownMotionQuestions:addHeader("Estimating Energy Input")
	upDownMotionQuestions:addText("Complete the following questions:")
	pbjQuestion = upDownMotionQuestions:addPotentialEnergy1Question();
	pbjQuestion:setMassQuestionText("Please enter the mass of the ball:")
	pbjQuestion:setHeightQuestionText("What was the height of the ball at its peak?")
	pbjQuestion:setEnergyQuestionText("How much energy input enabled the ball to reach this height?")
	pbjQuestion:setPbjSandwichQuestionText("A typical peanut butter jam (PBJ) sandwich contains 432 calories. How many throws could you perform with one PBJ sandwich?\n(1 calorie = 4.184 J)");
	upDownMotionQuestions:addCheckQuestion("Go to your demonstrator now and verify your results.")

	-- projectile motion
	local projectileMotionQuestions = scriptBuilder:create("Sheet")
	scriptBuilder:add(projectileMotionQuestions)
	projectileMotionQuestions:setTitle("Projectile Motion")
	projectileMotionQuestions:addHeader("Analysing graphs")
	projectileMotionQuestions:addText("The position-time graphs and the velocity-time graphs are created from your measurements. Use these graphs to complete the questions below.")
	projectileMotionQuestions:addYSpeedGraph(experimentProjectile)
	projectileMotionQuestions:addQuestion("How does the vertical velocity vary with time?") 
	projectileMotionQuestions:addQuestion("How would you draw a free body diagram of the ball at the peak of the trajectory?")
	projectileMotionQuestions:addQuestion("Compare the “vertical velocity-time” graphs of projectile motion, vertical linear motion and free fall. What are the similarities or the differences? (Hint: How does the direction of the moving ball affect the sign of velocity? Does the velocity vary or remain constant? Does the acceleration vary or remain constant?)")
	
	projectileMotionQuestions:addHeader("Analysing the horizontal velocity in projectile motion")
	projectileMotionQuestions:addXSpeedGraph(experimentProjectile)
	projectileMotionQuestions:addText("Use the “horizontal velocity-time” graph to answer the following question:")
	projectileMotionQuestions:addQuestion("Does the horizontal velocity vary or remain constant? What about the horizontal acceleration?")

	projectileMotionQuestions:addHeader("Estimating the horizontal and vertical accelerations")
	projectileMotionQuestions:addQuestion("Using the velocity-time graphs, estimate the horizontal and vertical accelerations. How did you arrive to your estimates.")
	
	local graphLayout = projectileMotionQuestions:addHorizontalGroupLayout()
	projectileMotionQuestions:addXSpeedGraph(experimentProjectile, graphLayout)
	projectileMotionQuestions:addYSpeedGraph(experimentProjectile, graphLayout)
	
	projectileMotionQuestions:addHeader("Sources of uncertainties")
	projectileMotionQuestions:addQuestion("How do your estimates of horizontal and vertical accelerations compare with your expected values? What are your expected values? If there is any difference, discuss the sources of uncertainties.")
	projectileMotionQuestions:addCheckQuestion("Go to your demonstrator now and verify your results.")
end


function onBuildExperimentScript(scriptBuilder)
	local takeVideoSheet = scriptBuilder:create("Sheet")
	scriptBuilder:add(takeVideoSheet)
	takeVideoSheet:setTitle("Camera Experiment:")
	cameraExperimentView = takeVideoSheet:addCameraExperiment();
	cameraExperimentView:setDescriptionText("Please take a video:")
	cameraExperimentView:setRequestedResolution(352, 288)
	cameraExperimentView:setRecordingFrameRate(1)
	local cameraExperiment = cameraExperimentView:getExperiment()

	local experimentAnalysis = scriptBuilder:create("MotionAnalysis")
	scriptBuilder:add(experimentAnalysis)
	experimentAnalysis:setTitle("Analyze the Video:")
	experimentAnalysis:setExperiment(cameraExperiment)
	experimentAnalysis:setDescriptionText("Please tag data points from the video:")

	local calculateYSpeed = scriptBuilder:create("CalculateYSpeed")
	calculateYSpeed:setExperiment(cameraExperiment)
	scriptBuilder:add(calculateYSpeed)

	local graphSheet = scriptBuilder:create("Sheet")
	scriptBuilder:add(graphSheet)
	graphSheet:setTitle("Graphs")
	-- some standard graphs:
	graphSheet:addMotionAnalysisGraph(cameraExperiment):showXVsYPosition()
	graphSheet:addMotionAnalysisGraph(cameraExperiment):showTimeVsXSpeed()
	graphSheet:addMotionAnalysisGraph(cameraExperiment):showTimeVsYSpeed(cameraExperiment)
	--[[ build a custom graph, possible axis are:
	time, x-position, y-position, x-speed, y-speed
	--]]
	local graph = graphSheet:addMotionAnalysisGraph(cameraExperiment)
	graph:setTitle("Height vs. Time")
	graph:setXAxisContent("time")
	graph:setYAxisContent("y-position")
	
	local question = scriptBuilder:create("Sheet")
	scriptBuilder:add(question)
	question:setTitle("Potential Energy Question")
	local graph = question:addMotionAnalysisGraph(cameraExperiment)
	graph:setTitle("Height vs. Time")
	graph:setXAxisContent("time")
	graph:setYAxisContent("y-position")
	pbjQuestion = question:addPotentialEnergy1Question()
	pbjQuestion:setMass(1)
	pbjQuestion:setHeightQuestionText("What was the height of the ball at its peak?")
	pbjQuestion:setEnergyQuestionText("How much energy input enabled the ball to reach this height?")
	question:addText("Please export your data:")
	question:addExportButton()

end

/**
 * Lablet: Physics Experiments on the Tablet
 * <p>
 * <h2>Overview:</h2>
 * <h3>Single Experiments:</h3>
 * <p>
 * Different experiments can be implemented using a plugin architecture
 * {@link nz.ac.aucklanduni.physics.lablet.experiment.ExperimentPlugin}. There is currently one common
 * {@link nz.ac.aucklanduni.physics.lablet.ExperimentAnalyserActivity} to analyze the experiments. However, since there
 * is currently only on {@link nz.ac.aucklanduni.physics.lablet.camera.CameraExperiment} this is not well tested to
 * work with different experiments.
 * </p>
 * <p>
 * Experiments can be managed in the {@link nz.ac.aucklanduni.physics.lablet.ExperimentHomeActivity}. For example,
 * start a new or resume and delete an old experiment.
 * </p>
 *
 * <h3>Scripts (Lab Activities):</h3>
 * <p>
 * It is possible to script a whole lab activity using a lua script. The script can have multiple pages including
 * questions, text, experiments, analysis... The script runs in the
 * {@link nz.ac.aucklanduni.physics.lablet.script.ScriptRunnerActivity}.
 * </p>
 * <p>
 * Scripts can be managed in the {@link nz.ac.aucklanduni.physics.lablet.ScriptHomeActivity}. For example,
 * start a new or resume and delete an old script.
 * </p>
 */
package nz.ac.aucklanduni.physics.lablet;
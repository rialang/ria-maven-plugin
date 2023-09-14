package ria;

import ria.lang.compiler.ria;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class is a Maven plugin that compiles Ria source files. It is executed during the compile phase of the Maven build lifecycle.
 * It takes the source directories containing the sources to be compiled, the directory for compiled classes, and the classpath as input parameters.
 * It then compiles the Ria source files using the Ria compiler and outputs the compiled classes to the specified output directory.
 */
@Mojo(name = "compile", defaultPhase = LifecyclePhase.COMPILE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class RiaCompilerMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The source directories containing the sources to be compiled.
     */
    @Parameter(defaultValue = "${project.compileSourceRoots}", readonly = true, required = true)
    private List<String> compileSourceRoots;

    /**
     * The directory for compiled classes.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true, readonly = true)
    private File outputDirectory;

    @Parameter( defaultValue = "${plugin}", readonly = true )
    private PluginDescriptor descriptor;

    /**
     * Constructs a new instance of the RiaCompilerMojo class.
     */
    public RiaCompilerMojo() {
        super();
    }

    public void execute() throws MojoExecutionException {

        List<String> runtimeClasspathElements;
        try {
            runtimeClasspathElements = project.getCompileClasspathElements();
        } catch(DependencyResolutionRequiredException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        ClassRealm realm = descriptor.getClassRealm();

        final String classPath = runtimeClasspathElements.stream().collect(Collectors.joining(":"));

        for(String element : runtimeClasspathElements) {
            File elementFile = new File(element);
            try {
                realm.addURL(elementFile.toURI().toURL());
            } catch(MalformedURLException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }

        try {
            getLog().info("Setting classpath to: " + classPath);
            if(compileSourceRoots != null) {
                for(String srcDir : compileSourceRoots) {
                    getLog().info("Compiling directory: " + srcDir);
                    ria.main(new String[] {"-d", outputDirectory.getAbsolutePath(), "-preload", "ria/lang/std:ria/lang/io", "-cp", classPath, srcDir});
                }
            }
        } catch(Exception e) {
            getLog().error(e);
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}

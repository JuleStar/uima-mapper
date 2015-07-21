package uima.sandbox.mapper.engines;

import java.io.FileInputStream;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import uima.sandbox.mapper.resources.Mapping;
import fr.univnantes.lina.UIMAProfiler;

public class Mapper extends JCasAnnotator_ImplBase {

	
	// parameters
	public static final String PARAM_SOURCE = "Source";
	@ConfigurationParameter(name = PARAM_SOURCE, mandatory=true)
	private String source;
	
	public static final String PARAM_TARGET = "Target";
	@ConfigurationParameter(name = PARAM_TARGET, mandatory=true)
	private String target;

	public static final String PARAM_UPDATE = "Update";
	@ConfigurationParameter(name = PARAM_UPDATE, mandatory=true)
	private Boolean update;
	
	public static final String PARAM_FILE = "File";
	@ConfigurationParameter(name = PARAM_FILE, mandatory=false)
	private String file;
	
	// resources
	@ExternalResource(key = Mapping.KEY_MAPPING)
	private Mapping mapping;
	
	
	
	private Type sourceType;
	private Feature sourceFeature;
	private Type targetType;
	private Feature targetFeature;
	
	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		try {
			
			// Parameters
			this.source = (String) context.getConfigParameterValue(PARAM_SOURCE);
			this.target = (String) context.getConfigParameterValue(PARAM_TARGET);
			this.update = (Boolean) context.getConfigParameterValue(PARAM_UPDATE);
			
			if (this.mapping == null) {
				this.mapping = (Mapping) context.getResourceObject(Mapping.KEY_MAPPING);

				String path = (String) context.getConfigParameterValue(PARAM_FILE);
	            if (path != null) {
	            	FileInputStream inputStream = new FileInputStream(path);
	        		this.getContext().getLogger().log(Level.INFO, "Loading  " + path);
	        		this.mapping.load(inputStream);
	            }
			}
			
		} catch (Exception e) {
            throw new ResourceInitializationException(e);
		}
	}
	
	
	@Override
	public void process(JCas cas) throws AnalysisEngineProcessException {
		UIMAProfiler.getProfiler("AnalysisEngine").start(this, "process");
		this.setSource(cas);
		this.setTarget(cas);
		AnnotationIndex<Annotation> index = cas.getAnnotationIndex(this.sourceType);
        FSIterator<Annotation> iter = index.iterator();
        while (iter.hasNext()) {
        	Annotation annotation = iter.next();
        	String source = null;
        	if (this.sourceFeature == null) { 
        		source = annotation.getCoveredText();
        	} else {
        		source = annotation.getStringValue(this.sourceFeature);
        	}
        	if (source != null) { 
        		String target = this.mapping.get(source);
        		if (target != null) { 
        			if (this.update.booleanValue()) {
    					this.update(cas, annotation, this.targetFeature, target);
    				} else {
    					this.create(cas, this.targetFeature, annotation.getBegin(), annotation.getEnd(), target);
    				}        			
        		}
            }
        }
        UIMAProfiler.getProfiler("AnalysisEngine").stop(this, "process");
	}

	
	private void setSource(JCas cas) throws AnalysisEngineProcessException {
		try {
		String[] items = this.source.split(":");
		if (items.length == 2) {
			this.sourceType = cas.getRequiredType(items[0].trim());
			this.sourceFeature = cas.getRequiredFeature(this.sourceType, items[1].trim());
		} else {
			this.sourceType = cas.getRequiredType(source);
			this.sourceFeature = null;
		}
		} catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}
	}
	
	
	private void setTarget(JCas cas) throws AnalysisEngineProcessException {
		try {
		String source = this.target;
		String[] items = source.split(":");
		if (items.length == 2) {
			this.targetType = cas.getRequiredType(items[0].trim());
			this.targetFeature = cas.getRequiredFeature(this.targetType, items[1].trim());
		} else {
			this.targetType = cas.getRequiredType(source);
			this.targetFeature = null;
		}
		} catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}
	}
	

	private void update(JCas cas, Annotation annotation, Feature feature, String value) {
		annotation.setStringValue(feature,value);
	}
	
	private void create(JCas cas, Feature feature, int begin, int end, String value) {
		Type type = feature.getDomain();
		AnnotationFS annotation = cas.getCas().createAnnotation(type, begin, end);
		annotation.setStringValue(feature,value);
		cas.addFsToIndexes(annotation);
	}
	
}

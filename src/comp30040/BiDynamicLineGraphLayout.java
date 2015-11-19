package comp30040;


import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.graph.Graph;
import org.apache.commons.collections15.Transformer;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Random;

/**
 * Created by rich on 26/10/2015.
 * @param <V>
 * @param <E>
 */
public class BiDynamicLineGraphLayout<V, E> extends AbstractLayout<String, String> {    
    public BiDynamicLineGraphLayout(BiDynamicLineGraph<V, E> g){
        super(g);
    }

    public BiDynamicLineGraphLayout(BiDynamicLineGraph<V, E> g, Dimension size){
        super(g, size);
    }

    public BiDynamicLineGraphLayout(BiDynamicLineGraph<V,E> g, Transformer<String,Point2D> initializer){
        super(g, initializer);
    }

    public BiDynamicLineGraphLayout(BiDynamicLineGraph<V,E> g, Transformer<String,Point2D> initializer,
    Dimension size) {
        super(g, initializer, size);
    }

    @Override
    public void initialize() {
        Point2D p = new Point2D.Double(25, 25);
        double eventSpacingX = (1440/graph.getNumberOfActors())*2.5;
        double eventSpacingY = 900/graph.getNumberOfEvents()*2.5;
        Random randomGenerator = new Random();
        for(NetworkEvent e : graph.getEvents()){
            int numberOfActorSeen = 0;
            for(Actor a: e.getActorsAtEvent()){
                Point2D pp = new Point2D.Double(p.getX(), p.getY());
                pp.setLocation(p.getX() + (eventSpacingX*Integer.parseInt(a.getLabel().substring(1))),
                              p.getY() );
                this.setLocation(a+e.getLabel(), pp);
                numberOfActorSeen++;
            }
            p.setLocation(p.getX(), p.getY() + eventSpacingY);
        }
    }

    @Override
    public void reset() {

    }
}
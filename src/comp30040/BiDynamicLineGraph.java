/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package comp30040;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 *
 * @author rich
 * @param <V>
 * @param <E>
 */
public class BiDynamicLineGraph<V, E> extends SparseGraph<V, E> {
    private GraphImporter imp = null;
    
    public BiDynamicLineGraph(){
        vertex_maps = new LinkedHashMap<>();
        directed_edges = new LinkedHashMap<>();
        undirected_edges = new LinkedHashMap<>();
    }
    
    public BiDynamicLineGraph(GraphImporter imp){
        vertex_maps = new LinkedHashMap<>();
        directed_edges = new LinkedHashMap<>();
        undirected_edges = new LinkedHashMap<>();
        this.imp = imp;
        genrateGraphFromImp();
    }
    
    public V getVertex(Actor a, NetworkEvent e){
        if (this.containsVertex((V) new VertexBDLG(a, e)))
            return (V) new VertexBDLG(a, e);
        return null;
    }
    
    public int getInDegree(VertexBDLG v){
        if (!containsVertex((V)v))
            throw new IllegalArgumentException(v + " is not a vertex in this graph");
        return vertex_maps.get(v)[INCOMING].keySet().size();
    }
    
    public int getOutDegree(VertexBDLG v){
        if (!containsVertex((V)v))
            throw new IllegalArgumentException(v + " is not a vertex in this graph");
        return vertex_maps.get(v)[OUTGOING].keySet().size();
    }
    
    public int getNumberOfActors(){
        return this.imp.getNumberOfActors();
    }
    
    public Actor[] getActors(){
        return this.imp.getActors();
    }
    
    public boolean isActorAtEvent(Actor a, NetworkEvent e){
        for(NetworkEvent event : imp.getActorsEvents(a))
            if(e.equals(event))
                return true;
        return false;
    }
    
    public int getNumberOfEvents(){
        return this.imp.getEvents().length;
    }
    
    public NetworkEvent[] getEvents(){
        return this.imp.getEvents();
    }
    
    public NetworkEvent[] getActorsEvents(Actor a){
        return this.imp.getActorsEvents(a);
    }
    
    public Graph<String, String> getOneModeActorGraph(){
        Graph<String, String> newOneModeG = new SparseGraph<>();
        for(Actor a : imp.getActors())
        {
            newOneModeG.addVertex(a.getLabel());
            for(NetworkEvent e : imp.getEvents())
            {
                if(e.isActorAtEvent(a))
                {
                    for(Actor acAtE : e.getActorsAtEvent()){
                        if(!a.equals(acAtE))
                            newOneModeG.addEdge(a.getLabel() + acAtE.getLabel(),
                                    a.getLabel(), acAtE.getLabel());
                    }
                }
            }
                
        }
        return newOneModeG;
    }
     
    public Graph<String, String> getOneModeEventGraph(){
        Graph<String, String> newOneModeG = new SparseGraph<>();
        for(NetworkEvent e : imp.getEvents())
        {
            newOneModeG.addVertex(e.getLabel());
            for(Actor a : e.getActorsAtEvent())
            {
                for(NetworkEvent eOther : imp.getEvents()){
                    if(!e.equals(eOther) && eOther.isActorAtEvent(a))
                    {
                        newOneModeG.addEdge(e.getLabel() + eOther.getLabel(),
                                            e.getLabel(), eOther.getLabel());
                    }
                }
            }
                
        }
        return newOneModeG;
    }
    
    @Override
    public boolean addVertex(V v)
    {
        if(this.containsVertex(v) || v == null)
            return false;
        this.vertex_maps.put(v, new LinkedHashMap[]{new LinkedHashMap<>(), 
                                                    new LinkedHashMap<>(),
                                                    new LinkedHashMap<>()});
        return true;
    }
    
    public Collection<V> getSuccessors(V v, EdgeType ed)
    {
        if(!this.containsVertex(v))
            return null;
        Collection<V> allSuccessors  = new ArrayList<>();
        if(ed == null || ed == EdgeType.DIRECTED)
            allSuccessors.addAll(this.vertex_maps.get(v)[OUTGOING].keySet());
        if(ed == null || ed == EdgeType.UNDIRECTED)
            allSuccessors.addAll(this.vertex_maps.get(v)[INCIDENT].keySet());
        return Collections.unmodifiableCollection(allSuccessors);
    }
    
    @Override
    public Collection<V> getSuccessors(V v)
    {
        return this.getSuccessors(v, null);
    }
    
    public EdgeType getEdgeType(V vOne, V vTwo){
        if(this.vertex_maps.get(vOne)[INCIDENT].containsKey(vTwo))
            return EdgeType.UNDIRECTED;
        return EdgeType.DIRECTED;
    }
    
    private void genrateGraphFromImp(){
        for(NetworkEvent e : imp.getEvents()){
            for(Actor a : e.getActorsAtEvent())
            {
                this.addVertex((V)new VertexBDLG(a, e));
            }
            
        }
        for(V v : this.getVertices())
        {
            for(V vv : this.getVertices())
            {
                if(((VertexBDLG) v).getEvent().equals(((VertexBDLG) vv).getEvent())){
                    this.addEdge((E)(v.toString() + vv.toString()),
                                 v, vv, EdgeType.UNDIRECTED);
                }
                else if( ((VertexBDLG) v).getActor().equals(((VertexBDLG) vv).getActor())
                        && ((VertexBDLG) v).getEvent().getEventId() < ((VertexBDLG) vv).getEvent().getEventId()
                        && this.getInDegree((VertexBDLG) v) <= 1
                        && this.getOutDegree((VertexBDLG) v) < 1)
                {
                    this.addEdge((E)(v.toString() + vv.toString()),
                                 v, vv, EdgeType.DIRECTED);
                }
            }
        }
    }

}

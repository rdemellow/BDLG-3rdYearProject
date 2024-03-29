/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package comp30040;

import edu.uci.ics.jung.graph.util.EdgeType;

/**
 *
 * @author rich
 */
public class PathPair {
    public VertexBDLG v;
    public EdgeType et;
    
    public PathPair(VertexBDLG v, EdgeType et){
        this.v = v;
        this.et = et;
    }
    
    public VertexBDLG getVertex(){
        return v;
    }
    
    public EdgeType getEdgeType(){
        return et;
    }
    
    public void setVertex(VertexBDLG v){
        this.v = v;
    }
    
    public void setEdgeType(EdgeType et){
        this.et = et;
    }
    
    @Override
    public String toString(){
        String path = "->";
        if(et == null)
            path = "";
        else if(et == EdgeType.UNDIRECTED)
            path = "-";
        return this.v.toString() + path;
    }
}
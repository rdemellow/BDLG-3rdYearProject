/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package comp30040;

import edu.uci.ics.jung.graph.util.EdgeType;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author rich
 */

public class PathFinder {
    private BiDynamicLineGraph graph = null;
    
    public PathFinder(BiDynamicLineGraph g){
        this.graph = g;
    }
    public void printPaths(){
        for(String p : paths){
            System.out.println(p);
        }
    }
    
    private ArrayList<String> paths = new ArrayList<>();
    public ArrayList<String> getPathsFrom(VertexBDLG i, Actor j, String currentPathString){
        Collection<VertexBDLG> currentVUndirectedEdges = graph.getSuccessors(i);
        if(currentVUndirectedEdges.size() < 1) return null;
        
        //1.get all undirected edges
        //2.pick any vertical
        //3.if veriacl goes to actor go to 6
        //4.pick undirected
        //5.go to 2
        //6.end
        for(VertexBDLG v: currentVUndirectedEdges){
            if(!i.equals(v)
               && graph.getEdgeType(graph.findEdge(i, v)) == EdgeType.UNDIRECTED)
            {
                if(j.equals(v.getActor()))
                {
                //ArrayList<VertexBDLG> tmpArray = new ArrayList<>();
                //tmpArray.add(v);
                //paths.add(tmpArray);
                    currentPathString += i + "-" + v;
                    paths.add(currentPathString);
                }
                else
                {
                    for(Object vv: graph.getSuccessors(v)){
                        if(!v.equals(vv) && graph.getEdgeType(graph.findEdge(v, vv)) == EdgeType.DIRECTED){
                            String s = i + "-" + v + "->";
                            getPathsFrom((VertexBDLG) vv, j, currentPathString + s);
                        }
                    }
                }
            }   
        }
        return paths;
    }
}

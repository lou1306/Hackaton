package com.gssi.cs32.hackaton.server;

import com.gssi.cs32.hackaton.models.Building;

import java.util.List;

/**
 * Created by luca on 01/06/17.
 */

public interface IServer {
    public List<Building> GetBuildings();
}

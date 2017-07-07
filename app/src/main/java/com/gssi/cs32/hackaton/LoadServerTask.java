/**
 * Created by luca on 06/07/17.
 */

package com.gssi.cs32.hackaton;

import com.gssi.cs32.hackaton.server.IServer;
import com.gssi.cs32.hackaton.server.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LoadServerTask extends android.os.AsyncTask<InputStream, Integer, IServer> {
    @Override
    protected IServer doInBackground(InputStream... params) {
        try {
            String mopsStr = readInputStream(params[0]);
            String qgisStr = readInputStream(params[1]);
            return new Server(mopsStr, qgisStr);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String readInputStream(InputStream stream) throws IOException{
        BufferedReader in = new BufferedReader(new InputStreamReader(stream));
        String str;
        StringBuilder buf=new StringBuilder();
        while ((str=in.readLine()) != null) {
            buf.append(str);
        }
        return buf.toString();
    }

}

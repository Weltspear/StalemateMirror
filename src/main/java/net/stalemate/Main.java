/*
 * Stalemate Game
 * Copyright (C) 2022 Weltspear
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.stalemate;

import net.stalemate.networking.client.Client;
import net.stalemate.networking.server.Server;

import java.io.File;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IllegalAccessException {

        File f = new File("grass32");
        if (!f.exists()){
            throw new IllegalAccessException("grass32 doesn't exist");
        }

        System.out.println("Stalemate Version: Alpha v0.2.3");

        while (true) {
            Scanner scanner = new Scanner(System.in);

            String menu = """
                    ##################
                        Stalemate
                     1: Play
                     2: Start a server
                    ##################
                     """;

            System.out.println(menu);
            System.out.println(">");

            int x = scanner.nextInt();

            if (x == 1) {
                Client client = new Client();
                client.start_client();
            }
            if (x == 2) {
                Server server = new Server();
                server.start_server();
            }
        }

    }
}

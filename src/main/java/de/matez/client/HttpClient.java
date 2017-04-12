package de.matez.client;

import java.util.List;

import feign.Feign;
import feign.gson.GsonDecoder;

public class HttpClient {

  public static void main(final String[] args) {

    final GitHub github =
        Feign.builder().decoder(new GsonDecoder()).target(GitHub.class, "https://api.github.com");

    // Fetch and print a list of the contributors to this library.
    final List<Contributor> contributors = github.contributors("OpenFeign", "feign");
    for (final Contributor contributor : contributors) {
      System.out.println(contributor.login + " (" + contributor.contributions + ")");
    }

  }


}

package com.chimbori.snacktroid;

import org.jsoup.nodes.Element;

/**
 * Class which encapsulates the data from an image found under an element
 */
class ImageResult {

  public final String src;
  public final Integer weight;
  private final String title;
  private final int height;
  private final int width;
  private final String alt;
  private final boolean noFollow;
  public Element element;

  public ImageResult(String src, Integer weight, String title, int height, int width, String alt, boolean noFollow) {
    this.src = src;
    this.weight = weight;
    this.title = title;
    this.height = height;
    this.width = width;
    this.alt = alt;
    this.noFollow = noFollow;
  }
}

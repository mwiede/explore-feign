package de.matez.client;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import feign.Client;
import feign.Feign;
import feign.Request;
import feign.Request.Options;
import feign.Response;
import feign.RetryableException;
import feign.Retryer;
import feign.Util;
import feign.codec.ErrorDecoder;
import feign.gson.GsonDecoder;

@RunWith(MockitoJUnitRunner.class)
public class FeignTest {

  @Mock
  Client clientMock;

  @Test
  public void testSuccess() throws IOException {

    when(clientMock.execute(any(Request.class), any(Options.class))).thenReturn(
        Response.builder().status(200).headers(Collections.<String, Collection<String>>emptyMap())
            .build());

    final GitHub github =
        Feign.builder().client(clientMock).decoder(new GsonDecoder())
            .target(GitHub.class, "https://api.github.com");

    github.contributors("OpenFeign", "feign");

    verify(clientMock, times(1)).execute(any(Request.class), any(Options.class));
  }

  @Test
  public void testDefaultRetryerGivingUp() throws IOException {

    when(clientMock.execute(any(Request.class), any(Options.class))).thenThrow(
        new UnknownHostException());

    final GitHub github =
        Feign.builder().client(clientMock).decoder(new GsonDecoder())
            .target(GitHub.class, "https://api.github.com");

    try {
      github.contributors("OpenFeign", "feign");
      fail("not failing");
    } catch (final Exception e) {
    } finally {
      verify(clientMock, times(5)).execute(any(Request.class), any(Options.class));
    }
  }

  @Test
  public void testRetryerAttempts() throws IOException {

    when(clientMock.execute(any(Request.class), any(Options.class))).thenThrow(
        new UnknownHostException());

    final int maxAttempts = 3;

    final GitHub github =
        Feign.builder().client(clientMock).decoder(new GsonDecoder())
            .retryer(new Retryer.Default(1, 100, maxAttempts))
            .target(GitHub.class, "https://api.github.com");

    try {
      github.contributors("OpenFeign", "feign");
      fail("not failing");
    } catch (final Exception e) {
    } finally {
      verify(clientMock, times(maxAttempts)).execute(any(Request.class), any(Options.class));
    }
  }

  @Test
  public void testCustomRetryConfigByErrorDecoder() throws IOException {

    when(clientMock.execute(any(Request.class), any(Options.class))).thenReturn(
        Response.builder().status(409).headers(Collections.<String, Collection<String>>emptyMap())
            .build(),
        Response.builder().status(200).headers(Collections.<String, Collection<String>>emptyMap())
            .build());

    class RetryOn409ConflictStatus extends ErrorDecoder.Default {

      @Override
      public Exception decode(final String methodKey, final Response response) {
        if (409 == response.status()) {
          return new RetryableException("getting conflict and retry", null);
        } else
          return super.decode(methodKey, response);
      }

    }

    final GitHub github =
        Feign.builder().client(clientMock).decoder(new GsonDecoder())
            .errorDecoder(new RetryOn409ConflictStatus())
            .target(GitHub.class, "https://api.github.com");


    github.contributors("OpenFeign", "feign");

    verify(clientMock, times(2)).execute(any(Request.class), any(Options.class));

  }

  @Test
  public void test409Error() throws IOException {

    when(clientMock.execute(any(Request.class), any(Options.class))).thenReturn(
        Response.builder().status(409).headers(Collections.<String, Collection<String>>emptyMap())
            .build(),
        Response.builder().status(200).headers(Collections.<String, Collection<String>>emptyMap())
            .build());

    final GitHub github =
        Feign.builder().client(clientMock).decoder(new GsonDecoder())
            .target(GitHub.class, "https://api.github.com");

    try {
      github.contributors("OpenFeign", "feign");
      fail("not failing");
    } catch (final Exception e) {
    } finally {
      verify(clientMock, times(1)).execute(any(Request.class), any(Options.class));
    }

  }

  @Test
  public void test400ErrorWithRetryAfterHeader() throws IOException {

    when(clientMock.execute(any(Request.class), any(Options.class))).thenReturn(
        Response
            .builder()
            .status(400)
            .headers(
                Collections.singletonMap(Util.RETRY_AFTER,
                    (Collection<String>) Collections.singletonList("1"))).build(),
        Response.builder().status(200).headers(Collections.<String, Collection<String>>emptyMap())
            .build());

    final GitHub github =
        Feign.builder().client(clientMock).decoder(new GsonDecoder())
            .target(GitHub.class, "https://api.github.com");


    github.contributors("OpenFeign", "feign");

    verify(clientMock, times(2)).execute(any(Request.class), any(Options.class));

  }
}

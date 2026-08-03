package org.envel.immersiveportalspaperized.proxy.net;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.envel.immersiveportalspaperized.shared.net.IRequestHandler;
import org.envel.immersiveportalspaperized.shared.net.encryption.EncryptedObjectStream;
import org.envel.immersiveportalspaperized.shared.net.encryption.EncryptedObjectStreamFactory;
import org.envel.immersiveportalspaperized.shared.net.encryption.IEncryptedObjectStream;

/**
 * Guice bindings for the proxy module.
 */
public class ProxyModule extends AbstractModule {
   @Override
   protected void configure() {
      this.bind(IPortalServer.class).to(PortalServer.class);
      this.bind(IRequestHandler.class).to(ProxyRequestHandler.class);
      this.install(new FactoryModuleBuilder().implement(IClientHandler.class, ClientHandler.class).build(IClientHandler.Factory.class));
      this.install(new FactoryModuleBuilder().implement(IEncryptedObjectStream.class, EncryptedObjectStream.class).build(EncryptedObjectStreamFactory.class));
   }
}

package org.envel.immersiveportalspaperized.bukkit.net;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.envel.immersiveportalspaperized.shared.net.IRequestHandler;
import org.envel.immersiveportalspaperized.shared.net.encryption.EncryptedObjectStream;
import org.envel.immersiveportalspaperized.shared.net.encryption.EncryptedObjectStreamFactory;
import org.envel.immersiveportalspaperized.shared.net.encryption.IEncryptedObjectStream;

public class NetworkModule extends AbstractModule {
   @Override
   public void configure() {
      this.install(new FactoryModuleBuilder().implement(IEncryptedObjectStream.class, EncryptedObjectStream.class).build(EncryptedObjectStreamFactory.class));
      this.bind(IPortalClient.class).to(PortalClient.class);
      this.bind(IRequestHandler.class).to(ClientRequestHandler.class);
      this.bind(IClientReconnectHandler.class).to(ClientReconnectHandler.class);
   }
}

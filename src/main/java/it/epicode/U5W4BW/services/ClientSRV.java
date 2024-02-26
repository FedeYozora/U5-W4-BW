package it.epicode.U5W4BW.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import it.epicode.U5W4BW.entities.Address;
import it.epicode.U5W4BW.entities.Client;
import it.epicode.U5W4BW.exceptions.BadRequestException;
import it.epicode.U5W4BW.exceptions.NotFoundException;
import it.epicode.U5W4BW.exceptions.UUIDNotFoundException;
import it.epicode.U5W4BW.payloads.NewClientDTO;
import it.epicode.U5W4BW.repositories.ClientDAO;
import it.epicode.U5W4BW.repositories.MunicipalityDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class ClientSRV {
    @Autowired
    private ClientDAO clientDAO;

    @Autowired
    private MunicipalityDAO municipalityDAO;

    @Autowired
    private Cloudinary cloudinaryUploader;


    public Page<Client> getClients(int pageNum, int size, String orderBy) {
        if (size > 100) size = 100;
        Pageable pageable = PageRequest.of(pageNum, size, Sort.by(orderBy));
        return clientDAO.findAll(pageable);

    }

    public Client saveClient(NewClientDTO newClient) {
        clientDAO.findByEmail(newClient.email()).ifPresent(user -> {
            throw new BadRequestException("Email " + newClient.email() + " is already used!");
        });
        Client client = new Client(
                newClient.businessName(),
                newClient.vatNumber(),
                newClient.email(),
                newClient.verifiedEmail(),
                newClient.phoneNumber(),
                newClient.contactEmail(),
                newClient.contactName(),
                newClient.contactSurname(),
                newClient.contactPhoneNumber(),
                newClient.type(),
                this.getAddress(newClient),
                this.getAddress(newClient)
        );
        return clientDAO.save(client);
    }

    public Client getClientById(UUID id) {
        return clientDAO.findById(id).orElseThrow(() -> new UUIDNotFoundException(id));
    }

    public Client updateClientById(NewClientDTO updatedClient, UUID id) {
        Client found = getClientById(id);
        found.setBusinessName(updatedClient.businessName());
        found.setVatNumber(updatedClient.vatNumber());
        found.setEmail(updatedClient.email());
        found.setVerifiedEmail(updatedClient.verifiedEmail());
        found.setPhoneNumber(updatedClient.phoneNumber());
        found.setContactEmail(updatedClient.contactEmail());
        found.setContactName(updatedClient.contactName());
        found.setContactSurname(updatedClient.contactSurname());
        found.setContactPhoneNumber(updatedClient.contactPhoneNumber());
        found.setType(updatedClient.type());
        found.setRegisteredAddress(this.getAddress(updatedClient));
        found.setHeadquartersAddress(this.getAddress(updatedClient));
        clientDAO.save(found);
        return found;
    }

    public void deleteClient(UUID id) {
        Client found = getClientById(id);
        clientDAO.delete(found);
    }

    public Client findClientByEmail(String email) {
        return clientDAO.findByEmail(email).orElseThrow(() -> new NotFoundException(email));

    }

    public String uploadAvatar(MultipartFile image, UUID id) throws IOException {
        String url = (String) cloudinaryUploader.uploader().upload(image.getBytes(),
                ObjectUtils.emptyMap()).get("url");

        Client found = this.getClientById(id);
        found.setClientLogo(String.valueOf(url));
        clientDAO.save(found);
        return url;
    }

    public Address getAddress(NewClientDTO newClientDTO){
        return new Address(
                newClientDTO.street(),
                newClientDTO.streetNumber(),
                newClientDTO.city(),
                newClientDTO.zipCode(),
                municipalityDAO.findByName(newClientDTO.municipalityName()));
    }


}

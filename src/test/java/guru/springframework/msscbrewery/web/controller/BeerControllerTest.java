package guru.springframework.msscbrewery.web.controller;

import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.constraints.ConstraintDescriptions;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import guru.springframework.msscbrewery.services.BeerService;
import guru.springframework.msscbrewery.web.model.BeerDto;

@ExtendWith(RestDocumentationExtension.class)
@AutoConfigureRestDocs
@WebMvcTest(BeerController.class)
@ComponentScan(basePackages = "guru.springframework.msscbrewery.web.mappers")
public class BeerControllerTest {

    @MockBean
    BeerService beerService;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    BeerDto validBeer;

    @BeforeAll
    public void setUp() {
        validBeer = BeerDto.builder().id(UUID.randomUUID())
                .beerName("Beer1")
                .beerStyle("PALE_ALE")
                .upc(123456789012L)
                .build();
    }

    @Test
    public void getBeer() throws Exception {
        given(beerService.getBeerById(any(UUID.class))).willReturn(validBeer);

        mockMvc.perform(
        		get("/api/v1/beer/" + validBeer.getId().toString()).accept(MediaType.APPLICATION_JSON)
        		)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        .andExpect(jsonPath("$.id", is(validBeer.getId().toString())))
        .andExpect(jsonPath("$.beerName", is("Beer1")))
        .andDo(
        		document(
        				"v1/beer-get",
        				pathParameters(
        						parameterWithName("beerId").description("UUID of desired beer to get.")
        						),
        				responseFields(
        						fieldWithPath("id").description("Id of Beer"),
        						fieldWithPath("version").description("Version number"),
        						fieldWithPath("createdDate").description("Date Created"),
        						fieldWithPath("lastModifiedDate").description("Date Updated"),
        						fieldWithPath("beerName").description("Beer Name"),
        						fieldWithPath("beerStyle").description("Beer Style"),
        						fieldWithPath("upc").description("UPC of Beer"),
        						fieldWithPath("price").description("Price"),
        						fieldWithPath("quantityOnHand").description("Quantity On hand")
        						)
        				)
        		);
    }

    @Test
    public void handlePost() throws Exception {
        //given
    	ConstrainedFields fields = new ConstrainedFields(BeerDto.class);
    	
        BeerDto beerDto = validBeer;
        beerDto.setId(null);
        BeerDto savedDto = BeerDto.builder().id(UUID.randomUUID()).beerName("New Beer").build();
        String beerDtoJson = objectMapper.writeValueAsString(beerDto);

        given(beerService.saveNewBeer(any())).willReturn(savedDto);

        mockMvc.perform(
        		post("/api/v1/beer/")
        		.contentType(MediaType.APPLICATION_JSON)
        		.content(beerDtoJson)
        		)
        .andExpect(status().isCreated())
        .andDo(
        		document(
        				"v1/beer-new",
        				requestFields(
        						fields.withPath("id").ignored(),
        						fields.withPath("version").ignored(),
        						fields.withPath("createdDate").ignored(),
        						fields.withPath("lastModifiedDate").ignored(),
        						fields.withPath("beerName").description("Name of the beer"),
        						fields.withPath("beerStyle").description("Style of Beer"),
        						fields.withPath("upc").description("Beer UPC").attributes(),
        						fields.withPath("price").description("Beer Price"),
        						fields.withPath("quantityOnHand").ignored()
        						)
        				)
        		);

    }

    @Test
    public void handleUpdate() throws Exception {
        //given
        BeerDto beerDto = validBeer;
        beerDto.setId(null);
        String beerDtoJson = objectMapper.writeValueAsString(beerDto);

        //when
        mockMvc.perform(put("/api/v1/beer/" + UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(beerDtoJson))
                .andExpect(status().isNoContent());

        then(beerService).should().updateBeer(any(), any());

    }
    
    private static class ConstrainedFields {
    	private final ConstraintDescriptions constraintDescriptions;
    	
    	ConstrainedFields(Class<?> input) {
    		this.constraintDescriptions = new ConstraintDescriptions(input);
    		
    	}
    	
    	private FieldDescriptor withPath(String path) {
    		return fieldWithPath(path).attributes(
    				key( "constraints").value(
    						StringUtils.collectionToDelimitedString(
    								this.constraintDescriptions.descriptionsForProperty(path),
    								". "
    								)
    						)
    				);
    	}
    }
}
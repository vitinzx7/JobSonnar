# JobSonnar

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=111111)
![JavaScript](https://img.shields.io/badge/JavaScript-ES6+-F7DF1E?logo=javascript&logoColor=111111)
![Vite](https://img.shields.io/badge/Vite-8-646CFF?logo=vite&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-Wrapper-C71A36?logo=apachemaven&logoColor=white)
![GitHub](https://img.shields.io/badge/GitHub-JobSonnar-181717?logo=github&logoColor=white)
![Status](https://img.shields.io/badge/status-em%20desenvolvimento-F59E0B)

O JobSonnar é uma aplicação web que pesquisa vagas por cargo e reúne resultados de diferentes fontes em uma única tela. O projeto nasceu como um buscador simples e está evoluindo para permitir buscas salvas e alertas de novas oportunidades.

## Problema que o projeto resolve

Quem procura estágio ou uma vaga júnior costuma repetir a mesma pesquisa em vários sites. Além do tempo gasto, cada plataforma apresenta os dados de um jeito diferente.

O JobSonnar centraliza essa primeira etapa: o usuário informa o cargo, o backend consulta as fontes disponíveis e devolve uma lista padronizada para o frontend.

## Funcionalidades atuais

- Busca de vagas por cargo ou palavra-chave.
- Resultados reunidos de duas fontes externas.
- Resposta JSON simplificada com nome, cidade, data e link da vaga.
- Exibição das vagas em cards responsivos.
- Estados visuais de carregamento, erro, busca vazia e nenhum resultado.
- Validação dos links externos antes de exibi-los ao usuário.
- Chave da API mantida no backend por variável de ambiente.

## Tecnologias

| Camada | Tecnologias |
| --- | --- |
| Backend | Java 21, Spring Boot, Spring Web MVC e RestClient |
| Frontend | React, JavaScript, Vite, HTML e CSS |
| Testes e qualidade | JUnit, Maven e ESLint |
| Integrações | APIs externas de vagas |

## Estrutura do projeto

```text
JobSonnar/
├── frontend/                         # Aplicação React
│   ├── public/                       # Arquivos públicos e favicon
│   └── src/
>>>> main
│       ├── App.jsx                   # Busca, estados e renderização das vagas
│       ├── App.css                   # Estilos dos componentes da página
│       ├── index.css                 # Estilos globais
│       └── main.jsx                  # Ponto de entrada do React
├── src/
│   ├── main/
│   │   ├── java/jobsonnar/
│   │   │   ├── controller/           # Recebe as requisições HTTP
│   │   │   ├── service/              # Coordena a busca e reúne os resultados
│   │   │   ├── provider/             # Comunicação com cada fonte de vagas
│   │   │   ├── dto/                  # Formatos de entrada e saída de dados
│   │   │   ├── model/                # Modelo usado na desserialização
│   │   │   └── JobSonnarApplication.java
│   │   └── resources/
│   │       └── application.properties # Configurações do backend
│   └── test/java/jobsonnar/           # Testes automatizados do backend
├── pom.xml                            # Dependências e configuração Maven
├── mvnw                               # Maven Wrapper para Linux/macOS
└── mvnw.cmd                           # Maven Wrapper para Windows
```

## Executando localmente

### Pré-requisitos

- Java 21

- Uma chave válida da API Jooble

### Backend

Na raiz do projeto:

```bash
export JOOBLE_API_KEY="sua_chave_aqui"
./mvnw spring-boot:run
```

O backend ficará disponível em `http://localhost:8080`.

### Frontend

Em outro terminal:

```bash
cd frontend
npm install
npm run dev
```

A interface ficará disponível em `http://localhost:5173`.

## API

```http
GET /jobs?query=java
```

Exemplo do formato retornado:

```json
[
  {
    "name": "Java Developer",
    "city": "São Paulo",
    "jobUrl": "https://example.com/job/123",
    "publishedDate": "2026-07-18"
  }
]
```

## Decisões importantes

- As APIs externas retornam estruturas diferentes. DTOs específicos transformam esses dados em um único formato para o frontend.
- O retorno original possuía muitos campos desnecessários. O `JobResponseDto` limita a resposta ao que a interface realmente usa.
- O frontend permite apenas links HTTPS dos domínios esperados e abre as vagas com proteção contra acesso à página de origem.
- Segredos não são enviados ao navegador: a chave da API é lida pelo Spring a partir de `JOOBLE_API_KEY`.

## Limitações atuais

- A URL do backend e a origem permitida pelo CORS ainda estão configuradas para o ambiente local.
- Uma falha em uma fonte externa ainda pode interromper toda a busca.
- As chamadas externas ainda não possuem timeout definido.
- O endpoint ainda precisa de validação de entrada, cache e limitação de requisições.
- O projeto ainda não salva buscas nem envia notificações.

## Próximos passos

- [ ] Concluir a separação de cada fonte em seu próprio provider.
- [ ] Preparar frontend e backend para deploy por variáveis de ambiente.
- [ ] Manter resultados parciais quando uma fonte estiver indisponível.
- [ ] Adicionar validação, timeout, cache e rate limiting.
- [ ] Salvar buscas e preferências no PostgreSQL.
- [ ] Criar alertas automáticos de novas vagas por e-mail.
- [ ] Adicionar autenticação antes de armazenar dados pessoais.
- [ ] Ampliar testes e automatizar as verificações com CI.

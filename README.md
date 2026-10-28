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

## Monitor automático de vagas

A seção **Suas vagas guardadas** permite salvar cargos, consultar o histórico, filtrar por fonte e marcar vagas como vistas. As buscas continuam salvas depois de reiniciar o backend. Remover uma busca interrompe seu acompanhamento e preserva as vagas já coletadas.

O monitor consulta a Gupy e as páginas de empresas configuradas a cada hora, enquanto o backend estiver em execução. LinkedIn e Indeed entram pela importação dos alertas de e-mail recebidos em uma pasta IMAP dedicada. Os filtros desses alertas são definidos nas próprias plataformas; os cargos cadastrados no JobSonnar controlam a coleta da Gupy e das empresas.

| Fonte | Integração |
| --- | --- |
| Gupy | HTML público da busca, extraindo `props.pageProps.initialJobList` do `__NEXT_DATA__`. Substitui o endpoint antigo, que estava retornando 404. |
| Empresas | Páginas indicadas em `application-monitor.yml`, com JSON-LD `JobPosting` ou seletores CSS configurados. |
| LinkedIn e Indeed | Links individuais presentes no HTML ou texto dos alertas recebidos por e-mail, sem acessar as contas dessas plataformas. |

### Começar

1. Copie `.env.example` para `.env`. O banco local H2 em `data/` funciona sem instalar PostgreSQL nem fornecer chaves de APIs.
2. Inicie o backend e o frontend conforme as instruções abaixo.
3. Cadastre cargos na seção do monitor. A primeira rodada ocorre dez segundos após iniciar o backend; as seguintes acontecem a cada hora. Buscas cadastradas depois dessa primeira rodada entram na próxima execução.
4. Clique em **Atualizar histórico** para consultar o banco; esse botão não dispara novas consultas aos sites.

`MONITOR_INTERVAL_MS` define a pausa entre rodadas. `MONITOR_ENABLED=false` pausa a coleta globalmente. O monitor reaproveita Java/Spring e executa em uma instância do backend; para funcionar com o notebook desligado, hospede o processo em um servidor com armazenamento persistente.

### Páginas de empresas

Copie `monitor.example.yml` para `application-monitor.yml` e informe as páginas reais que deseja acompanhar. O exemplo usa domínios fictícios comentados: nenhuma empresa vem cadastrada por padrão. Reinicie o backend depois de alterar o arquivo.

O coletor lê o HTML recebido, sem executar JavaScript. Páginas que carregam as vagas apenas no navegador precisam de um conector específico. Ele coleta as vagas presentes na página configurada, sem seguir automaticamente todos os links do site nem percorrer paginação.

### Alertas de LinkedIn e Indeed

1. Crie alertas de vagas nas próprias plataformas, com cargos e regiões desejados, e ative a entrega por e-mail.
2. Crie uma pasta de e-mail chamada `JobSonnar` e uma regra para mover esses alertas para ela.
3. Configure `ALERT_EMAIL_HOST`, `ALERT_EMAIL_USERNAME`, `ALERT_EMAIL_PASSWORD` e `ALERT_EMAIL_ENABLED=true` no `.env`. Use credencial de aplicativo quando o provedor oferecer esse recurso. O importador usa IMAPS; provedores que exigem exclusivamente OAuth precisam de uma integração adicional.
4. Reinicie o backend. O estado da importação aparece na tela do monitor.

A pasta é aberta em modo somente leitura: o importador não envia, move, apaga nem marca mensagens como lidas. Ele relê até 1000 mensagens e elimina repetições pelo identificador da vaga. Arquive mensagens já importadas para manter a pasta dentro desse limite. Não são guardados corpos de e-mail no banco.

Links de rastreamento opacos, que escondem o endereço individual da vaga, não são seguidos. Alguns modelos de alerta podem exigir ajustes no parser. Datas de publicação, empresa e cidade ausentes no alerta ficam sem informação; a data de recebimento não é usada como data de publicação.

### Histórico e acesso

O histórico diferencia **publicação** de **primeira coleta**. Na primeira execução, todas as vagas encontradas são não vistas, inclusive anúncios antigos. Nas seguintes, links já conhecidos atualizam apenas a última coleta e preservam a marca de vista. Vagas distintas com o mesmo cargo são mantidas. URLs diferentes de uma mesma oportunidade podem exigir uma regra de identidade específica para a fonte; não se descarta uma vaga apenas porque seu título é parecido.

Para usar PostgreSQL, configure `DATABASE_URL=jdbc:postgresql://host:5432/jobsonnar`, `DATABASE_USER` e `DATABASE_PASSWORD`. O esquema é criado na inicialização. A configuração não transfere automaticamente dados do H2. Faça backup de `data/` se usar o banco local; em hospedagem, monte um volume persistente ou use PostgreSQL.

Por padrão, a API `/monitor` aceita apenas conexões locais. Antes de expor o backend ou colocá-lo atrás de um proxy, configure `MONITOR_API_TOKEN` com um segredo longo e use HTTPS. Na interface, informe esse token em **Acesso ao monitor em servidor remoto**. Ele fica apenas na memória da página. A busca original `/jobs` continua com seu comportamento anterior.

Limites da coleta: a integração da Gupy captura a página mais recente de resultados por cargo; não é uma varredura de todas as vagas. As páginas de empresas e os alertas também cobrem apenas o que suas fontes entregam. Falhas de fonte aparecem no estado de cada busca e não apagam o histórico.

### Verificar

```bash
./mvnw test
cd frontend
npm run lint
npm run build
```

Os testes novos verificam persistência após reabrir o banco, identificação de links repetidos, preservação de vagas diferentes, interpretação de HTML/JSON-LD e e-mails multipart, isolamento de falhas e controle de acesso ao monitor. As fontes externas são simuladas nos testes; credenciais IMAP e páginas de empresas precisam ser validadas no ambiente configurado.

## Problema que o projeto resolve

Quem procura estágio ou uma vaga júnior costuma repetir a mesma pesquisa em vários sites. Além do tempo gasto, cada plataforma apresenta os dados de um jeito diferente.

O JobSonnar centraliza essa primeira etapa: o usuário informa o cargo, o backend consulta as fontes disponíveis e devolve uma lista padronizada para o frontend.

## Funcionalidades atuais

- Busca de vagas por cargo ou palavra-chave.
- Resultados reunidos de três fontes externas.
- Busca com foco em Brasília/DF e raio de 5 km como recorte inicial.
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

- Uma chave válida da API Jooble, se quiser habilitar essa fonte (opcional para o monitor)
- Credenciais da API Adzuna, se quiser habilitar essa fonte no ambiente local

### Backend

Na raiz do projeto:

```bash
set -a
source .env
set +a
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
GET /jobs?query=java&location=Bras%C3%ADlia%2FDF&radiusKm=5
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
- Segredos não são enviados ao navegador: as chaves são lidas pelo Spring a partir de variáveis de ambiente.
- A busca atual aceita cargo, localidade e raio, com Brasília/DF e 5 km como padrão inicial.

## Limitações atuais

- A URL do backend e a origem permitida pelo CORS ainda estão configuradas para o ambiente local.
- Uma falha em uma fonte externa não derruba a busca inteira; o backend devolve os resultados parciais das demais fontes.
- As chamadas externas ainda não possuem timeout definido.
- O endpoint ainda precisa de validação de entrada, cache e limitação de requisições.
- O recorte de 5 km ainda é um primeiro passo baseado em localidade, sem geocodificação completa do ponto de origem.
- O monitor salva buscas e vagas; envio de notificações ainda não foi implementado.

## Próximos passos

- [ ] Preparar frontend e backend para deploy por variáveis de ambiente.
- [ ] Concluir a normalização geográfica completa do recorte de 5 km.
- [ ] Adicionar validação, timeout, cache e rate limiting.
- [x] Salvar buscas e histórico de vagas com JDBC, H2 local e PostgreSQL configurável.
- [ ] Criar alertas automáticos de novas vagas por e-mail.
- [ ] Adicionar autenticação antes de armazenar dados pessoais.
- [ ] Ampliar testes e automatizar as verificações com CI.

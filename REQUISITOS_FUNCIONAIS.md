# SORA - Rede Social de Viajantes

## Requisitos Funcionais

---

## FUNCIONALIDADES ESSENCIAIS (Prioridade 1)

### Autenticação e Usuários

**RF-001 - Cadastro de Usuário**

- Permitir que novos usuários criem conta no sistema
- Campos: email, senha, username, nome, foto de perfil, bio
- Validação de email e username único no sistema

**RF-002 - Autenticação de Usuário**

- Login/logout com email e senha

**RF-003 - Gerenciamento de Perfil**

- Visualizar e editar dados pessoais
- Alterar foto de perfil, nome, bio

### Criação de Conteúdo

**RF-004 - Criação de Post com Fotos**

- Upload de múltiplas fotos por post
- Seleção de localização (país/cidade) obrigatória
- Adição de descrição/legenda

**RF-005 - Criação de Post com Vídeos**

- Upload de vídeos curtos
- Seleção de localização (país/cidade) obrigatória  
- Adição de descrição/legenda

**RF-006 - Opção de Duplicação de Posts**

- Ao postar em viagem compartilhada, escolher se post vai apenas para o perfil do dono ou se duplica para ambos os perfis
- Posts duplicados aparecem nos dois perfis sem repetição visual
- Indicação visual de posts compartilhados vs. próprios

### Sistema de Viagens por País

**RF-007 - Criação Automática de Viagem por País**

- Sistema cria automaticamente agrupamento por país no primeiro post
- Viagens são organizações de países visitados
- Cada país visitado fica permanentemente no perfil

**RF-008 - Globo Principal (Tela Inicial)**

- Tela inicial com globo 3D mostrando markers nos países onde usuários seguidos publicaram recentemente
- Clique no marker do país mostra posts recentes dos usuários seguidos naquele país
- Interface de descoberta e exploração de conteúdo

**RF-009 - Globo do Perfil do Usuário**

- Globo 3D no perfil do usuário mostrando países visitados pintados
- Clique no país abre subperfil com posts daquele país
- Estatísticas visuais de progresso de viagem individual

**RF-010 - Subperfil por País**

- Visualização dedicada dos posts de cada país
- Organização cronológica dos posts por país
- Acesso através do globo do perfil do usuário

**RF-011 - Categorização por Coleções**

- Posts organizados em: Geral, Culinária, Eventos, Outros
- Seleção obrigatória de categoria ao criar post
- Filtro por categoria dentro de cada país

**RF-012 - Filtros por Localização**

- Filtrar posts por cidade específica dentro do país

**RF-013 - Gamificação de Viagem**

- Contador de países/cidades visitados individual
- Ranking de países/cidades mais visitados por pessoas que segue
- Progresso visual de exploração global no globo 3D, com países já visitados sendo pintados no perfil do usuário

**RF-014 - Tela Explorar**

- Globo 3D para explorar posts de outros usuários
- Seleção de país mostra posts recentes organizados por mais curtidos
- Descoberta de novos destinos através de posts populares

---

## FUNCIONALIDADES SOCIAIS (Prioridade 2)

**RF-015 - Sistema de Seguir Usuários**

- Seguir/deixar de seguir outros usuários
- Sistema unilateral (não precisa de confirmação)
- Contador de seguidores e seguindo

**RF-016 - Busca de Usuários**

- Pesquisar usuários por nome ou username
- Filtros por localização visitada
- Sugestões de usuários para seguir

**RF-017 - Visualização de Perfil Público**

- Todos os perfis são públicos
- Visualizar globo de países visitados de outros usuários
- Estatísticas: países/cidades visitadas, total de seguidores

**RF-018 - Sistema de Likes em Posts**

- Curtir/descurtir posts
- Contagem total de likes visível
- Lista de usuários que curtiram

**RF-019 - Sistema de Comentários**

- Comentar em posts
- Respostas aninhadas (replies)
- Notificações para donos do post

**RF-020 - Compartilhamento de Viagem por País**

- Compartilhar acesso de postagem para país específico com outro usuário
- Convite por username ou busca
- Sistema de permissões temporárias

**RF-021 - Gerenciamento de Permissões**

- Aceitar/rejeitar convites de compartilhamento
- Visualizar lista de usuários com permissão
- Revogar permissões a qualquer momento

**RF-022 - Controle de Posts em Viagens Compartilhadas**

- Usuário que convidou pode excluir apenas seus próprios posts, não pode excluir duplicata
- Usuário que foi convidado pode excluir posts duplicados em seu perfil, e enquanto tiver permissão, pode excluir post no perfil do que convidou
- Posts mantidos mesmo após revogação de permissão

**RF-023 - Duplicação Inteligente**

- Posts duplicados não aparecem repetidos no mesmo perfil
- Marcação visual de posts compartilhados vs. próprios

**RF-024 - Lista de Seguidores**

- Visualizar usuários que seguem o perfil
- Links para perfis dos seguidores

**RF-025 - Lista de Seguindo**

- Visualizar usuários seguidos pelo perfil
- Links para perfis seguidos
- Opção de deixar de seguir

---

## FUNCIONALIDADES AVANÇADAS (Prioridade 3)

**RF-026 - Sistema de Notificações**

- Notificar sobre likes, comentários, novos seguidores
- Notificar convites e atividades de viagens colaborativas
- Configurações de notificação personalizáveis

**RF-027 - Modo Offline**

- Cache de posts visualizados recentemente
- Visualização básica sem conexão

**RF-028 - Sistema de Denúncias**

- Reportar conteúdo inadequado
- Sistema de moderação automatizado
- Processo de revisão manual

---

## MODELO DE VIAGENS COMPARTILHADAS

### Conceito Principal

**Viagens organizadas por país** - cada usuário tem automaticamente um agrupamento para cada país que visitou. Pode compartilhar permissão de postagem com outros usuários, sendo que essa permissão será por país.

### Sistema de Viagens por País

#### Viagens Pessoais por País

- **Criação**: Automática no primeiro post em cada país
- **Organização**: Um agrupamento por país visitado
- **Proprietário**: Apenas o usuário dono
- **Persistência**: Eternas, países visitados ficam permanentemente no perfil
- **Visualização**: Globo 3D com países pintados

#### Sistema de Compartilhamento

- **Funcionamento**: Dono da viagem pode dar permissão para outros usuários postarem em seu país
- **Convite**: Por username, com aceite/rejeição
- **Permissões**: Temporárias e revogáveis pelo dono a qualquer momento
- **Múltiplas Colaborações**: Um país pode ter permissões ativas para múltiplos usuários simultaneamente

### Fluxo de Postagem com Múltiplas Colaborações

1. **Seleção de Localização**: Usuário seleciona cidade para postar
2. **Detecção Automática**: Sistema identifica automaticamente que o país tem permissões compartilhadas ativas
   - Exemplo: Brasil compartilhado com Ana, Carlos e Pedro
3. **Seleção de Colaborador**:
   - Opção 1: "Apenas no meu perfil" (post pessoal)
   - Opção 2: Escolher um colaborador específico (Ana, Carlos OU Pedro)
4. **Opção de Duplicação** (apenas se colaborador selecionado):
   - "Postar apenas no perfil de [Nome]": Post aparece só no perfil do colaborador
   - "Postar em ambos os perfis": Post duplica para o seu perfil E do colaborador
5. **Post é criado** conforme seleção feita

### Regras de Integridade dos Posts

1. **Post não duplicado**: Aparece apenas no perfil do que postou
2. **Post duplicado**: Aparece em ambos os perfis, cada um controla sua própria cópia para sempre. Mas enquanto tiver permissão, colaborador pode editar o post do dono.
3. **Após revogação de permissão**:
   - Posts originais (não duplicados) permanecem no perfil do dono
   - Posts duplicados permanecem em ambos os perfis
   - Colaborador perde apenas a capacidade de criar novos posts no perfil do dono
4. **Exclusões independentes**: Excluir post duplicado em um perfil não afeta o outro

### Exemplo Prático com Múltiplas Colaborações

**Cenário**:

1. **Configuração Inicial**:
   - João visita Brasil → Sistema cria agrupamento "Brasil" no perfil do João
   - João compartilha permissão do Brasil com Ana, Carlos e Pedro
   - Juca também visita Brasil → Sistema cria agrupamento "Brasil" no perfil do Juca
   - Juca compartilha permissão do Brasil apenas com Ana
   - Todos aceitam os convites

2. **Situação de Postagem da Ana**:
   - Ana está no Rio de Janeiro e quer postar uma foto
   - Ana seleciona "Rio de Janeiro, Brasil" como localização
   - Sistema detecta que Ana tem permissões ativas no Brasil com 2 pessoas: João e Juca

3. **Fluxo de Decisão da Ana**:
   - **Opção A**: "Apenas no meu perfil" → Foto aparece só em "Ana - Brasil"
   - **Opção B**: Escolhe colaborar com João:
     - "Postar apenas no perfil do João" → Foto aparece só em "João - Brasil"
     - "Postar em ambos os perfis" → Foto aparece em "João - Brasil" E "Ana - Brasil"
   - **Opção C**: Escolhe colaborar com Juca:
     - "Postar apenas no perfil do Juca" → Foto aparece só em "Juca - Brasil"
     - "Postar em ambos os perfis" → Foto aparece em "Juca - Brasil" E "Ana - Brasil"
   - **Limitação**: Ana NÃO pode escolher João E Juca simultaneamente - apenas um por post

4. **Situação Posterior**:
   - Carlos também posta escolhendo "ambos os perfis" com João
   - Pedro posta escolhendo "apenas no perfil do João"
   - Ana posta algumas fotos com João e outras com Juca
   - Mais tarde, João revoga permissão de Ana
   - **Resultado**: Fotos duplicadas entre João e Ana permanecem em ambos os perfis
   - **Resultado**: Ana ainda pode postar colaborando com Juca (permissão ativa)
   - **Resultado**: Fotos de Pedro permanecem apenas no perfil do João
   - **Resultado**: Carlos ainda pode continuar postando (permissão ativa)

---

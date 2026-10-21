# GoalUnited — base Java + Playwright

Módulo independente da aplicação Swing/Robot existente. Java 17, Maven 3.9+ e Playwright 1.63.0. Não precisa de Video Speed Controller, OpenCV ou JNativeHook.

Esta primeira versão executa **um slot de treinamento da segunda-feira por vez**, às 10:00 ou 12:00, com opção de assistir ao segundo vídeo. Aguarda reprodução normal e confirmação visual da recompensa. Não acelera o vídeo nem altera os mecanismos de validação do jogo.

## Instalação

No PowerShell, CMD ou terminal, entre neste diretório (a raiz do repositório contém a versão antiga):

```sh
cd playwright-v2
mvn compile
mvn exec:java "-Dexec.mainClass=com.microsoft.playwright.CLI" "-Dexec.args=install chromium"
```

O navegador abre com um perfil exclusivo, `.gu-profile`, e guarda sua sessão localmente. Faça login manualmente. Não aponte esse diretório para o perfil habitual do Chrome. Para usar o Chrome instalado, acrescente `--channel chrome` aos argumentos.

## Primeiro teste: diagnóstico sem automação

```sh
mvn exec:java "-Dexec.args=--flow diagnose"
```

No navegador, abra **Time > Treinamento** e feche diálogos. Volte ao terminal e pressione ENTER. O aplicativo salva uma imagem normalizada do canvas em `runs/<data>/before.png` e `after.png` e encerra. O terminal e `recognition.txt` informam se cada slot está disponível, concluído ou não reconhecido, junto com o erro visual medido.

Use interface em **português**, zoom do navegador em **100%** e a janela fornecida pelo aplicativo. Os recortes visuais vêm das telas enviadas em 09/10/2026, com referências adicionais do título e dos ícones disponíveis no Chromium capturadas em 21/10/2026. O limite de erro continua em 2,5%; o reconhecimento usa o melhor resultado entre as referências explícitas de cada recorte, na mesma posição. O primeiro diálogo de recompensa também tem referências da captura real de 21/10. A oferta de bônus e as marcas de conclusão ainda precisam de validação nessa renderização. O canvas de referência mede **942 × 671**; tamanhos com a mesma proporção são normalizados. Outras proporções, alterações no layout, fonte, idioma ou estado interrompem o fluxo para recalibração. Redimensionamento proporcional também precisa de validação local, pois o jogo pode reorganizar controles.

## Treinamento: um ciclo

```sh
mvn exec:java "-Dexec.args=--flow training --slot 10 --bonus"
```

Faça login, abra **Time > Treinamento** manualmente e pressione ENTER no terminal. O aplicativo:

1. Reconhece o título da tela e o ícone de vídeo do slot escolhido.
2. Solicita o vídeo e aguarda o evento real `ended` do player em `#guVideoContainer`.
3. Reconhece o diálogo e clica em **Coletar recompensa**.
4. Reconhece a oferta do segundo vídeo; com `--bonus`, assiste e coleta também essa recompensa.
5. Confirma a marca de conclusão no slot e encerra.

Para o segundo slot, use `--slot 12`. Sem `--bonus`, a oferta é fechada após a primeira recompensa. Um slot que já mostra a marca de conclusão não será acionado novamente. Essa versão não recupera um bônus pendente de uma execução anterior; encerra ou recusa o estado para inspeção manual. Para retomar especificamente na **primeira recompensa já aberta** de um slot conhecido, use a opção abaixo.

Se uma execução parou após o primeiro vídeo e o jogo ainda apresentar a primeira recompensa desse slot, deixe o diálogo aberto e execute:

```sh
mvn compile exec:java "-Dexec.args=--flow training --slot 10 --bonus --resume-reward"
```

Pressione ENTER apenas com essa primeira recompensa aberta, sem coletá-la manualmente. A retomada exige o diálogo reconhecido, coleta e segue para a oferta do bônus; não solicita novamente o primeiro vídeo. A opção não restaura diálogos após recarregar o jogo, não identifica o slot por trás do modal e não serve para a recompensa do segundo vídeo. Só a use quando souber qual slot gerou essa primeira recompensa. Se o diálogo não estiver visível, a retomada recusa a tela sem clicar. Para um novo slot disponível, execute sem `--resume-reward`.

O tempo máximo é 180 segundos **por vídeo**, configurável com `--video-timeout 240` (máximo 600). Há limites de 30 segundos para o início, 45 segundos sem progresso e 20 segundos para cada transição de tela. Vídeo indisponível, erro no player ou diálogo diferente interrompe a execução; nenhum clique é repetido automaticamente. Se o player exigir fechar uma janela após o término e o jogo não abrir a recompensa sozinho, o fluxo para; esse estado ainda precisa ser mapeado.

Interrompa com **Ctrl+C**, ou crie um arquivo vazio chamado **STOP** neste diretório. Remova STOP antes da próxima execução. Em erro, `runs/<data>/error.png` mostra a página inteira, `error-canvas.png` preserva a última imagem normalizada realmente verificada e `canvas.txt` registra as dimensões CSS/capturadas. `error.txt` registra a exceção, incluindo erros por recorte nos limites de espera. Para calibrar novos recortes, prefira `error-canvas.png` em vez de recortar manualmente a captura da página inteira. Capturas e perfil ficam fora do Git e podem conter dados da conta.

## Localizar novos controles dentro do canvas

```sh
mvn exec:java "-Dexec.args=--flow map"
```

Depois de ENTER, clique normalmente no jogo. O terminal registra `GU_POINT x,y` relativo ao canvas 942 × 671. ENTER novamente encerra o mapeamento. O jogo recebe esses cliques: use apenas os controles que pretende executar. Os pontos (`points.log`) e as imagens inicial e final ficam em `runs/`.

Os pontos conhecidos abaixo servem para os próximos fluxos. **Somente o treinamento possui execução automática nesta versão**; navegação, Masterclass, recompensa geral e eventos precisam de recortes que confirmem seus estados antes de serem habilitados.

| Controle | X | Y |
|---|---:|---:|
| Time | 409 | 642 |
| Treinamento no menu | 414 | 586 |
| Segunda 10:00 — vídeo | 505 | 311 |
| Segunda 12:00 — vídeo | 505 | 371 |
| Coletar recompensa de treinamento | 471 | 448 |
| Assistir segundo vídeo | 471 | 543 |
| Fechar oferta de segundo vídeo | 707 | 111 |
| Abrir Masterclass | 12 | 214 |
| Assistir vídeo Masterclass | 331 | 471 |
| Fechar Masterclass | 748 | 142 |
| Abrir vídeos gerais | 20 | 165 |
| OK da recompensa geral | 483 | 491 |

Para adicionar uma tela, recorte áreas estáveis de `before.png` (título, botão, marca de estado), sem nomes, saldos ou prêmio variável. Adicione PNGs em `src/main/resources/templates`, suas posições em `Visuals`, e um fluxo que chame `CanvasDriver.await` e `click` com a lista de verificações. Use pelo menos título/descrição e controle alvo para diálogos; não automatize apenas por coordenadas. O limite de erro RGB é conservador (2,5%); se uma tela válida for recusada, inspecione a captura e atualize os recortes antes de aumentar tolerâncias.

## Arquitetura e verificação

- `Main`: CLI, perfil persistente, login manual e evidências.
- `CanvasDriver`/`Geometry`: proporção, imagem normalizada, cliques relativos, verificação de tela e STOP.
- `Visuals`: recortes fixos e comparação de pixels sem dependência nativa.
- `VideoMonitor`: observação de eventos de mídia confiáveis; ocultar o player ou emitir evento sintético não confirma conclusão.
- `TrainingFlow`: sequência limitada a um slot e no máximo dois vídeos.

```sh
mvn test
```

A referência de recompensa do Chromium foi reconstruída a partir da captura de página de 21/10, alinhando-a ao canvas antes do vídeo; a coleta real ainda precisa de confirmação. Os testes verificam esse recorte independentemente do prêmio variável e a retomada explícita sem repetir o primeiro vídeo.

Os testes abrem Chromium headless com uma página local e um WebM de um segundo, sem acessar o jogo. Exercitam duas reproduções reais, um ciclo sem bônus, slot já concluído, tela incorreta, interrupção, player removido e evento `ended` sintético. Também verificam a transformação de coordenadas e a distinção visual dos ícones.

O CI repete a instalação do navegador e os testes. **A validação com sua conta ainda precisa ocorrer localmente**, sobretudo disponibilidade de anúncios, oferta do bônus e renderização dos recortes. Não há agendamento diário, automação de login, recuperação automática, navegação por todos os menus ou suporte a eventos nesta base.

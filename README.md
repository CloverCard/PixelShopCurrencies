# PixelShopCurrencies
This is a Pixelmon Reforged server-sided sidemod that implements scoreboard custom currencies for shopkeepers, allowing the purchases of items and command uses.
![image](https://user-images.githubusercontent.com/121761085/210184299-63a76b6d-a0cb-4559-8842-39e5e17dd6c3.png)
## Commands
### /clbal <currency_id>
Displays your current balance of a particular scoreboard currency. In the event this currency does not exist, you'll be informed by the error message.

## Set Up and Configuration
### 1.) Download the Mod
You'll need to get the [latest release](https://github.com/CloverCard/PixelShopCurrencies/releases) and place it within your mods folder.
### 2.) Create the file structure for a Datapack that modifies shopkeepers and shopkeeper items or modify the [example provided](https://github.com/CloverCard/PixelShopCurrencies/blob/master/PixelShopCurrency-Example/PixelShopCurrency-Example.zip).
### 3.) Configure Shopkeeper Items
In order to make an item require a special currency, you'll need to give it the correct nbt tags via a datapack in the shopitems.json file. The three tags to be aware of are clovercost, clovercur, and clovercmd. 

- clovercur: This is a string value that holds the id name of your currency within the scoreboard.

- clovercost: This is an integer and will hold how much of the scoreboard currency the item will cost.

- clovercmd (optional): This is a string value that will hold the data for the command a player is purchasing. Keep in mind that including this tag will change the thing sold from an item to a command.

  - The first argument of the string must either be self or console. This will determine whether the command will be run as the player or the console.

  - The rest of the arguments will be like any other command other command

  - If you would like to use the name of the player who purchased the command anywhere in the arguments, substitute it with PLAYER, and the player's name will be used when the command runs.

**Below are three examples of how to configure items using a scoreboard currency I named vp.**
(Keep in mind that any display data you provide will be removed once you obtain the item!) 
```json
{
  "items": [
    {
      "id": "Test",
      "name": "pixelmon:master_ball",
      "nbtData": "{clovercost:5, clovercur:\"vp\", display:{\"Name\":'{\"text\":\"Master Ball - 5 VP\"}'}}",
      "buy": 0,
      "sell": 0
    },
    {
      "id": "Test2",
      "name": "pixelmon:master_ball",
      "nbtData": "{clovercost:5, clovercur:\"vp\", clovercmd:\"self say hello PLAYER!\", display:{\"Name\":'{\"text\":\"Greeting command - 5 VP\"}'}}",
      "buy": 0,
      "sell": 0
    },
    {
      "id": "Test3",
      "name": "pixelmon:master_ball",
      "nbtData": "{clovercost:1, clovercur:\"vp\", clovercmd:\"console pokegive PLAYER Bidoof\", display:{\"Name\":'{\"text\":\"Get Bidoof - 1 VP\"}'}}",
      "buy": 0,
      "sell": 0
    }
  ]
}
```
### 3.) Configure Shopkeepers
Now that the items have been registered, all you need to do is add them to a shopkeeper.json or make your own. Here is an example where I replaced the items of a preexisting shopkeeper.json file with the new ones added in the example above.
```json
{
  "data": {
    "type": "PokemartMain"
  },
  "textures": [
    {
      "name": "shopman2.png"
    },
    {
      "name": "shopman.png"
    },
    {
      "name": "shopman3.png"
    },
    {
      "name": "shopman4.png"
    },
    {
      "name": "shopman5.png"
    }
  ],
  "names": [
    {
      "name": "Charles"
    },
    {
      "name": "Eito"
    },
    {
      "name": "Mr. Benson"
    },
    {
      "name": "Mr. Wong"
    },
    {
      "name": "James"
    },
    {
      "name": "Digby"
    }
  ],
  "chat": [
    {
      "hello": "Welcome to my Pok?? Mart. I hope you find something you like!",
      "goodbye": "See you next time."
    },
    {
      "hello": "Hello, welcome to the Pok?? Mart. How may I help you?",
      "goodbye": "Thank you for shopping at the Pok?? Mart. Have a nice day."
    },
    {
      "hello": "Hello there! Welcome to my Pok?? Mart. I have only the best Pok??mon products for sale.",
      "goodbye": "Thank you for shopping at my store."
    },
    {
      "hello": "Welcome to my Pok?? Mart. Nice day for shopping, isn't it?",
      "goodbye": "Come back soon, won't you?"
    },
    {
      "hello": "Hello there, welcome to the Pok?? Mart! We have many specials today!",
      "goodbye": "Come see us again, won't you?"
    },
    {
      "hello": "Hey there, welcome to the Pok?? Mart. Looking for anything special?",
      "goodbye": "Okay, see ya next time."
    },
    {
      "hello": "How are you? Welcome to our Pok?? Mart! We've got some great gear for your Pok??mon.",
      "goodbye": "Ciao, my friend. Come back anytime."
    },
    {
      "hello": "Hello, check out our Pok?? Mart! We have some useful Pok??mon gear today.",
      "goodbye": "Come back again soon, okay?"
    },
    {
      "hello": "Hi, looking for the best Pok?? Mart? Well, you just found it!",
      "goodbye": "Leaving so soon? Well, make sure you come back even sooner!"
    }
  ],
  "items": [
    {
      "name": "Test"
    },
    {
      "name": "Test2"
    },
    {
      "name": "Test3"
    }
  ]
}
```
### 4.) Saving your new Datapack
Once you've saved the json files you've edited, all you need to do is zip up all the files into a proper datapack, and add it to your server's datapack folder.

### 5.) Boot Up your Server.
At this point, you're good to go. 

- If you haven't created a scoreboard before, here is an example of creating the vp currency used in the example
  - /scoreboard objectives add vp dummy

- To add to a player's score, here is another example:
  - /scoreboard players add CloverCard vp 10



